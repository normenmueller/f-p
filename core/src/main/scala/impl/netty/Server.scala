package silt
package impl
package netty

import java.util.concurrent.LinkedBlockingQueue

import scala.concurrent.{ ExecutionContext, Promise }
import ExecutionContext.Implicits.{ global => executor }

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.{ LogLevel, LoggingHandler => Logger }

import com.typesafe.scalalogging.{ StrictLogging => Logging }

trait Server extends AnyRef with silt.Server with Logging {

  self: silt.SiloSystem =>

  /** Promise the server is up and running.
    *
    * To fulfill this promise use `self` tied to [[silt.SiloSytem]].
    */
  protected def started: Promise[silt.SiloSystem]

  // Netty server constituents
  private val srvr = new ServerBootstrap
  private val boss = new NioEventLoopGroup
  private val wrkr = new NioEventLoopGroup

  // Worker for all incoming messages from all channels.
  private val receptor = new Receptor(self, new LinkedBlockingQueue[Incoming]())

  /* Initialize a [[Netty-based http://netty.io/wiki/user-guide-for-4.x.html]]
   * server.
   */
  logger.info("Server initializing...")
  srvr.group(boss, wrkr).channel(classOf[NioServerSocketChannel]).childHandler(
    new ChannelInitializer[SocketChannel]() {
      override def initChannel(ch: SocketChannel): Unit =
        ch.pipeline()
          .addLast(new Logger(LogLevel.INFO))
          // XXX LengthFieldBasedFrameDecoder ?
          .addLast(new SystemMessageDecoder())
          .addLast(new SystemMessageEncoder())
          .addLast(new Forwarder(receptor)) // XXX @Sharable ?
    })
  // XXX are those options necessary?
  //.option(ChannelOption.SO_BACKLOG.asInstanceOf[ChannelOption[Any]], 128) 
  //.childOption(ChannelOption.SO_KEEPALIVE.asInstanceOf[ChannelOption[Any]], true)
  logger.info("Server initializing done.")

  /** Start server.
    *
    * Start and bind server to accept incoming connections at port `at.port`.
    */
  override def run(): Unit = {
    logger.debug("Server start...")

    receptor.start()
    srvr.bind(at.port).sync()
    started success self

    logger.debug("Server start done.")
    logger.info(s"Server listining at port ${at.port}.")
  }

  /** Stop server.
    *
    * In Nety 4.0, you can just call `shutdownGracefully` on the
    * `EventLoopGroup` that manages all your channels. Then all ''existing
    * channels will be closed automatically'' and reconnection attempts should
    * be rejected.
    */
  override def stop(): Unit = {
    logger.info("Server stop...")

    receptor.stop()
    wrkr.shutdownGracefully()
    boss.shutdownGracefully()

    logger.info("Server stop done.")
  }

}

// vim: set tw=80 ft=scala:
