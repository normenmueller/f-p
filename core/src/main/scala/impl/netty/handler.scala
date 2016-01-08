package silt
package impl
package netty

import java.util.concurrent.BlockingQueue

import io.netty.channel.ChannelHandlerContext
//import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.util.ReferenceCountUtil

import com.typesafe.scalalogging.{ StrictLogging => Logging }

/* Forward incoming messages from Netty's event loop to [[Receptor]]. */
// private[netty] class Forwarder(receptor: Receptor) extends ChannelInboundHandlerAdapter with Logging {
private[netty] class Forwarder(receptor: Receptor) extends SimpleChannelInboundHandler[silt.Message] with Logging {

  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    logger.debug("Server inbound handler entered status `channelActive`.")
  }

  /* Forward incoming messages to internal [[mq]] queue.
   *
   * Be aware that messages are not released after the
   * `channelRead(ChannelHandlerContext, Object)` method returns automatically.
   *
   * In case of a default, Netty-based, ''server mode'' silo system realization,
   * the [[Receptor]] will release the message.
   *
   * In case of a default, Netty-based, ''client mode'' silo system realization,
   * XXX will release the message.
   */
  //override def channelRead(ctx: ChannelHandlerContext, msg: silt.Message): Unit = {
  override def channelRead0(ctx: ChannelHandlerContext, msg: silt.Message): Unit = {
    logger.debug("Server inbound handler entered status `channelRead`.")
    // receptor add Incoming(ctx, msg)
    //msg.asInstanceOf[ByteBuf].toString(io.netty.util.CharsetUtil.US_ASCII).trim() match {
    //msg.payload.trim() match {
    //  case "shutdown" => receptor.system.shutdown
    //  case _          => receptor add Incoming(ctx, msg)
    //}
  }

  // XXX Don't just close the connection when an exception is raised.
  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    cause.printStackTrace()
    ctx.close()
  }

}

/** Execute [[silt.impl.netty.Server server]]'s event loop.
  *
  * @param mq Message queue incomming messages are forwared to.
  */
private[netty] class Receptor(val system: silt.SiloSystem, mq: BlockingQueue[Incoming]) extends AnyRef with Logging /*with SendUtils*/ {

  def start(): Unit = {
    logger.debug("Receptor start ...")

    worker.setName(s"Receptor of ${system.name}")
    worker.setDaemon(true)
    worker.start()

    logger.debug("Receptor start done.")
  }

  def stop(): Unit = {
    logger.debug("Receptor stop ...")

    terminate = true
    worker.interrupt()
    worker.join()

    logger.debug("Receptor stop done.")
  }

  def add(msg: Incoming): Boolean =
    mq add msg

  @volatile private var terminate = false

  private val worker = new Thread {

    override def run(): Unit =
      while (!terminate) try mq.take() match {
        //case l: HandleIncomingLocal => handleIncomingLocal(l.msg, l.ctx, l.resultPromise)
        case Incoming(msg, ctx) => handleIncoming(msg, ctx)
        case _                  => // XXX: unexpected object in queue.
      } catch { case _: InterruptedException => /* continue to check `terminate` */ }

  }

  private def handleIncoming(ctx: ChannelHandlerContext, msg: Any): Unit = {
    //val in = msg.asInstanceOf[ByteBuf]
    //val bos = new ByteArrayOutputStream

    try {
      //while (in.isReadable()) bos.write(in.readByte().asInstanceOf[Int])
    } finally {
      ReferenceCountUtil.release(msg)
    }

  }

}

// vim: set tw=80 ft=scala:
