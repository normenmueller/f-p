package silt
package impl
package netty

import java.util.concurrent.{ BlockingQueue, LinkedBlockingQueue }

import scala.concurrent.Promise

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.{ LogLevel, LoggingHandler => Logger }

import com.typesafe.scalalogging.{ StrictLogging => Logging }

private[netty] trait Server extends AnyRef with silt.Server with Logging {

  self: silt.SiloSystem =>

  // Promise the server is up and running.
  def up: Promise[silt.SiloSystem]

  val srvr = new ServerBootstrap
  val boss = new NioEventLoopGroup
  val wrkr = new NioEventLoopGroup

  // Worker for all incoming messages from all channels.
  val receptor = new Receptor(self, new LinkedBlockingQueue[Incoming]())

  /* Initialize a [[Netty-based http://netty.io/wiki/user-guide-for-4.x.html]]
   * server.
   */
  logger.info("Server initializing...")
  srvr.group(boss, wrkr).channel(classOf[NioServerSocketChannel]).childHandler(
    new ChannelInitializer[SocketChannel]() {
      override def initChannel(ch: SocketChannel): Unit =
        ch.pipeline()
          .addLast(new Logger(LogLevel.INFO))
          .addLast(new Forwarder(receptor))
      // XXX Implement pickling with Netty Encoder/Decoder ChannelHandlers 
    })
  // XXX are those options necessary?
  //.option(ChannelOption.SO_BACKLOG.asInstanceOf[ChannelOption[Any]], 128) 
  //.childOption(ChannelOption.SO_KEEPALIVE.asInstanceOf[ChannelOption[Any]], true)
  logger.info("Server initializing done.")

  override def run(): Unit = {
    srvr.bind(at.port).sync()
    logger.info(s"Server listining at port ${at.port}.")

    up success self

    /* Wait until the server socket is closed.
     * Note: Blocks JVM termination if [[silt.SiloSystem#terminate]] omitted.
     * Note: No longer required due to [[silt.impl.netty.SiloSystem#hook]].
     */
    //cf.channel().closeFuture().sync()
  }

  override def stop(): Unit = {
    logger.info("Server shutdown...")

    /* In Nety 4.0, you can just call `shutdownGracefully` on the
     * `EventLoopGroup` that manages all your channels. Then all ''existing
     * channels will be closed automatically'' and reconnection attempts should
     * be rejected.
     */
    wrkr.shutdownGracefully()
    boss.shutdownGracefully()

    logger.info("Server shutdown done.")
  }

}

/** Execute [[silt.impl.netty.Server server]]'s event loop.
  *
  * @param mq Message queue incomming messages are forwared to.
  */
private[netty] class Receptor(val system: silt.SiloSystem, val mq: BlockingQueue[Incoming]) extends AnyRef with Runnable /*with SendUtils*/ {

  override def run(): Unit = ???

}

import io.netty.channel.ChannelInboundHandlerAdapter

/* Forward incoming messages from Netty's event loop to [[Receptor]]. */
private[netty] class Forwarder(receptor: Receptor) extends ChannelInboundHandlerAdapter with Logging {

  import io.netty.buffer.ByteBuf
  import io.netty.channel.ChannelHandlerContext
  import io.netty.util.CharsetUtil

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
  override def channelRead(ctx: ChannelHandlerContext, msg: Object): Unit = {
    logger.debug("Server inbound handler entered status `channelRead`.")
    msg.asInstanceOf[ByteBuf].toString(CharsetUtil.US_ASCII).trim() match {
      case "shutdown" => receptor.system.terminate // XXX mq add Terminate
      case _          => receptor.mq add Incoming(ctx, msg)
    }
  }

  // XXX Don't just close the connection when an exception is raised.
  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    cause.printStackTrace()
    ctx.close()
  }

}

// vim: set tw=80 ft=scala:
