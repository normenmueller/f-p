package silt
package impl

import java.util.concurrent.BlockingQueue

import io.netty.channel.{ ChannelFutureListener, ChannelHandler, ChannelHandlerContext }
import io.netty.channel.SimpleChannelInboundHandler

import com.typesafe.scalalogging.{ StrictLogging => Logging }

/* Forward incoming messages from Netty's event loop to [[silt.Processor silo
 * system's system message processor]].
 *
 * Be aware that due to the default constructor parameter of
 * [[io.netty.channel.SimpleChannelInboundHandler]] all messages will be
 * released by passing them to [[io.netty.util.ReferenceCountUtil#release(Object)]].
 */
// private[netty] class Forwarder(receptor: Receptor) extends ChannelInboundHandlerAdapter with Logging {
@ChannelHandler.Sharable
private[impl] class Forwarder( /*processor: Processor*/ ) extends SimpleChannelInboundHandler[silt.Message] with Logging {

  import logger._

  /* XXX Documentation is obsolete: Valid for ChannelInboundHandlerAdapter
   *
   * Forward incoming messages to internal [[mq]] queue.
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
    trace(s"Forwarder received message: $msg")
    // XXX processor process msg
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    import ChannelFutureListener._

    //cause.printStackTrace()

    // Don't just close the connection when an exception is raised.
    //ctx.close()

    val msg = s"${cause.getClass().getSimpleName()}: ${cause.getMessage()}"
    logger.error(msg)

    // XXX With the current setup of the channel pipeline, in order to respond
    // with a raw text message --- recall only a silo sytem can create system
    // messages due to `Id` --- additional encoder/ decoder are required.
    //if (ctx.channel().isActive())
    //  ctx.writeAndFlush(msg).addListener(CLOSE)
    //else ()

  }

}

// vim: set tw=80 ft=scala:
