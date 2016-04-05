package fp.backend.netty

import java.net.InetSocketAddress
import java.util.concurrent.{BlockingQueue, CountDownLatch}

import com.typesafe.scalalogging.{StrictLogging => Logging}
import fp.backend.WrappedMsg
import fp.backend.netty.handlers.{TransformHandler, PopulateHandler}
import fp.model._
import fp.util.AsyncExecution

import scala.concurrent.ExecutionContext

private[netty] class Receptor(incoming: BlockingQueue[NettyWrapper])
                             (implicit val ec: ExecutionContext, server: Server)
  extends Runnable with AsyncExecution with Logging {

  import logger._

  /** Responsible for controlling the status of the [[Receptor]] */
  private val counter = new CountDownLatch(1)

  /** Handle incoming [[fp.model.Message]]s by pattern matching on their
    * types and spawning [[scala.concurrent.Future]]s that will carry out
    * its processing. There should be a [[fp.backend.netty.handlers.Handler]]
    * for each type of message.
    */
  def handleMsg(wrapper: NettyWrapper, host: InetSocketAddress): Unit = {
    val NettyWrapper(ctx, msg) = wrapper
    msg match {
      case p: Populate[_] => PopulateHandler.handle(p, ctx)
      case m: Transform => TransformHandler.handle(m, ctx)
      case _ => error(s"We weren't able to handle $msg")
    }

  }

  import server.{ MessagingStatus, OnHoldMessages }

  private def updateStatusOf(status: MessagingStatus): MessagingStatus = {
    val (previousExpectedId, msgs) = status
    (previousExpectedId.increaseByOne, msgs)
  }

  private def processStoredMsgs(current: MessagingStatus, host: InetSocketAddress) = {

    @scala.annotation.tailrec
    def process(nextId: Int, msgs: OnHoldMessages): MessagingStatus = {
      val nextMsg = msgs.peek()
      if (nextMsg != null && nextMsg.msg.id.value == nextId) {
        msgs.poll()
        handleMsg(nextMsg, host)
        process(nextId + 1, msgs)
      } else {
        (MsgId(nextId), msgs)
      }
    }

    val (expectedId, onHold) = current
    process(expectedId.value, onHold)
  }

  /** Confirm the last sent message to a given host. The last message must have
    * been lost on its way since the client has sent again an already processed
    * message. Therefore, reconfirm it. This conforms to the ACK-Reply protocol. */
  def confirmAgainMsg(ctx: NettyContext): Unit = {
    val lastResponse = server.unconfirmedResponses(ctx.getRemoteHost)
    implicit val sp = lastResponse.getPickler
    implicit val sup = lastResponse.getUnpickler
    server.tell(ctx.channel, lastResponse.specialize)
  }

  @inline private def processingEnabled = counter.getCount > 0

  def start(): Unit = {
    trace("Receptor started.")

    while (processingEnabled) {

      val wrappedMsg = incoming.take
      debug(s"Receptor received: $wrappedMsg")
      val host = wrappedMsg.ctx.getRemoteHost

      val status = server.statusFrom(host)
      val (expectedId, onHoldMsgs) = status

      val msgId = wrappedMsg.msg.id.value
      val expectedMsgId = expectedId.value

      if (msgId == expectedMsgId) {
        /* Client and server are on the same page */
        handleMsg(wrappedMsg, host)
        val updatedStatus = updateStatusOf(status)
        server.statusFrom.update(host, updatedStatus)
        processStoredMsgs(updatedStatus, host)
      } else if (msgId == expectedMsgId - 1) {
        /* Message's already been processed */
        confirmAgainMsg(wrappedMsg.ctx)
      } else if (msgId > expectedMsgId) {
        /* Received a future message since its id is greater
         * than the expected. Store for future processing. */
        onHoldMsgs put wrappedMsg
      } else {
        error(Feedback.receptionAlgorithmFailed)
      }
    }
  }

  def stop(): Unit = {
    trace("Stopping receptor...")
    counter.countDown()
    trace("Receptor stopped.")
  }

  final override def run(): Unit = start()

}

