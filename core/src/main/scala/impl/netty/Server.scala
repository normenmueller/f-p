package silt
package impl
package netty

import java.util.concurrent.BlockingQueue

import scala.concurrent.Promise

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.{ LogLevel, LoggingHandler }

import com.typesafe.scalalogging.{ StrictLogging => Logging }

private[netty] trait Server extends AnyRef with Runnable with Logging {

  self: SiloSystem =>

  // Location where server is bound and started.
  def at: Host

  // Message queue incomming messages are forwared to.
  def mq: BlockingQueue[Incoming]

  // Promise the server is up and running.
  def up: Promise[SiloSystem]

  val srvr = new ServerBootstrap
  val boss = new NioEventLoopGroup
  val wrkr = new NioEventLoopGroup

  /* Initialize a [[Netty-based http://netty.io/wiki/user-guide-for-4.x.html]]
   * server.
   */
  logger.info("Server initializing...")
  srvr.group(boss, wrkr).channel(classOf[NioServerSocketChannel]).childHandler(
    new ChannelInitializer[SocketChannel]() {
      override def initChannel(ch: SocketChannel): Unit =
        ch.pipeline()
          .addLast(new LoggingHandler(LogLevel.INFO))
          // XXX Implement pickling with Netty Encoder/Decoder ChannelHandlers 
          .addLast(new ForwardInboundHandler(Server.this, mq))
    })
  // XXX are those options necessary?
  //.option(ChannelOption.SO_BACKLOG.asInstanceOf[ChannelOption[Any]], 128) 
  //.childOption(ChannelOption.SO_KEEPALIVE.asInstanceOf[ChannelOption[Any]], true)
  logger.info("Server initializing done.")

  def run(): Unit = {
    // Bind and start to accept incoming connections
    val cf = srvr.bind(at.port).sync()
    logger.info(s"Server listining at port ${at.port}.")

    up success self

    /* Wait until the server socket is closed.
     *
     * Note: Blocks JVM termination if [[silt.SiloSystem#terminate]] omitted.
     */
    cf.channel().closeFuture().sync()
  }

  def stop(): Unit = {
    logger.info("Server shutdown...")

    /* In Nety 4.0, you can just call `shutdownGracefully` on the
     * `EventLoopGroup` that manages all your channels. Then all existing
     * channels will be closed automatically and the reconnection attempt should
     * be rejected.
     */
    wrkr.shutdownGracefully()
    boss.shutdownGracefully()

    logger.info("Server shutdown done.")
  }

}

import io.netty.channel.ChannelInboundHandlerAdapter

/** Server-side channel inbound handler */
private[netty] class ForwardInboundHandler(srvr: Server, mq: BlockingQueue[Incoming]) extends ChannelInboundHandlerAdapter with Logging {

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
      case "shutdown" => srvr.stop // XXX mq add Terminate
      case _          =>           // XXX mq add Incoming(ctx, msg)
    }
  }

  // XXX Don't just close the connection when an exception is raised.
  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    cause.printStackTrace()
    ctx.close()
  }

}

// vim: set tw=80 ft=scala:
