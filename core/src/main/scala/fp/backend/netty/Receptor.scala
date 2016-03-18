package fp.backend.netty

import java.net.InetSocketAddress
import java.util.concurrent.{BlockingQueue, CountDownLatch}

import com.typesafe.scalalogging.{StrictLogging => Logging}
import fp.backend.netty.handlers.PopulateHandler
import fp.model.Populate
import fp.util.AsyncExecution

import scala.concurrent.ExecutionContext

private[netty] class Receptor(incoming: BlockingQueue[NettyWrapper])
                             (implicit val ec: ExecutionContext, server: Server)
  extends Runnable with AsyncExecution with Logging {

  import logger._

  /** Responsible for controlling the status of the [[Receptor]] */
  private val counter = new CountDownLatch(1)

  /* It will get inlined by the JVM since it's private, no overhead */
  private def processingEnabled = counter.getCount > 0

  def start(): Unit = {
    trace("Receptor started.")

    while (processingEnabled) {

      val wrappedMsg = incoming.take
      val host = wrappedMsg.ctx.getRemoteHost
      val (expectedMsg, msgs) = server.msgsFrom(host)

      val msgId = wrappedMsg.msg.id.value
      val expectedMsgId = expectedMsg.value

      if (msgId == expectedMsgId) {
        /* Client and server are on the same page */
        handleMsg(wrappedMsg, host)
      } else if (msgId == expectedMsgId - 1) {
        /* Message's already been processed */
        confirmAgainMsg(wrappedMsg.ctx)
      } else if (msgId > expectedMsgId) {
        /* Received a future message since its id is greater
         * than the expected. Store for future processing. */
        msgs put wrappedMsg
      } else {
        error(s"""
          |A message id less than `expectedMsgId.value - 1` has been received.
          |This means that the invariants of the protocol have been violated and
          |there's a bug either in the client or in the server.
          """.stripMargin
        )
      }

    }
  }

  /** Handle incoming [[fp.model.Message]]s by pattern matching on their types
    * and spawning [[scala.concurrent.Future]]s that will carry out its
    * processing. There should be a [[fp.backend.netty.handlers.Handler]]
    * for each type of message.
    */
  def handleMsg(wrapper: NettyWrapper, host: InetSocketAddress): Unit = {
    val NettyWrapper(ctx, msg) = wrapper

    msg match {
      case m: Populate[_] => PopulateHandler.handle(m, ctx)
      case _ => error(s"We weren't able to handle $msg")
    }

  }

  /** Confirm the last sent message to a given host. The last message must have
    * been lost on its way since the client has sent again an already processed
    * message. Therefore, reconfirm it. This is part of the ACK-Retry protocol. */
  def confirmAgainMsg(ctx: NettyContext): Unit = {
    val lastResponse = server.pendingOfConfirmation(ctx.getRemoteHost)
    implicit val sp = lastResponse.getPickler
    server.tell(ctx.channel, lastResponse.specialize)
  }

  def stop(): Unit = {
    trace("Stopping receptor...")
    counter.countDown()
    trace("Receptor stopped.")
  }

  final override def run(): Unit = start()

}

