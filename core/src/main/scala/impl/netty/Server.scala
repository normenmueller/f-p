package silt
package impl
package netty

import java.util.concurrent.BlockingQueue

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.{ ChannelFuture, ChannelHandler, ChannelHandlerContext, ChannelInboundHandlerAdapter, ChannelInitializer, EventLoopGroup }
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.{ LogLevel, LoggingHandler }

import com.typesafe.scalalogging.{ StrictLogging => Logging }

private[netty] class Server(val at: Host, queue: BlockingQueue[Incoming]) extends AnyRef with Runnable with Logging {

  private val srvr: ServerBootstrap = new ServerBootstrap
  private val boss: EventLoopGroup = new NioEventLoopGroup
  private val wrkr: EventLoopGroup = new NioEventLoopGroup

  /* Initialize a Netty-based server as described at
   * [[http://netty.io/wiki/user-guide-for-4.x.html]]
   */
  logger.info("Server initializing...")
  srvr.group(boss, wrkr).channel(classOf[NioServerSocketChannel]).childHandler(
    new ChannelInitializer[SocketChannel]() {
      override def initChannel(ch: SocketChannel): Unit =
        ch.pipeline()
          .addLast(new LoggingHandler(LogLevel.INFO))
          // XXX Implement pickling with Netty Encoder/Decoder ChannelHandlers 
          .addLast(new ForwardInboundHandler(queue))
    })
  // XXX are those options necessary?
  //.option(ChannelOption.SO_BACKLOG.asInstanceOf[ChannelOption[Any]], 128) 
  //.childOption(ChannelOption.SO_KEEPALIVE.asInstanceOf[ChannelOption[Any]], true)
  logger.info("Server initializing done.")

  def run(): Unit = {
    val cf = srvr.bind(at.port).sync()
    logger.info(s"Server listining at port ${at.port}.")

    // XXX Calling silo system should not return before server is up and running

    // wait until the server socket is closed
    cf.channel().closeFuture().sync()
  }

  def stop(): Unit = {
    logger.info("Server is shutting down...")

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

/** Server-side channel inbound handler */
private[netty] class ForwardInboundHandler(queue: BlockingQueue[Incoming]) extends ChannelInboundHandlerAdapter with Logging {

  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    logger.debug("Server inbound handler entered status `channelActive`.")
  }

  /* Forward incoming messages to internal [[queue]].
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
    //queue.add(Incoming(ctx, msg))
  }

  // XXX Don't just close the connection when an exception is raised.
  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    cause.printStackTrace()
    ctx.close()
  }

}

// vim: set tw=80 ft=scala:
