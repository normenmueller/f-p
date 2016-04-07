package fp
package backend
package netty

import java.net.InetSocketAddress
import java.util.concurrent.{ CountDownLatch, LinkedBlockingQueue }

import com.typesafe.scalalogging.{ StrictLogging => Logging }
import fp.model.{SiloSystemId, MsgId, Response, Message}
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.{ ChannelHandler, ChannelHandlerContext}
import io.netty.channel.{ ChannelInitializer, SimpleChannelInboundHandler }
import io.netty.handler.logging.{ LogLevel, LoggingHandler => Logger }

import scala.concurrent.Promise
import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext.Implicits.{ global => executor }
import scala.pickling.Unpickler

private[netty] trait Server extends backend.Server with SiloWarehouse
  with MessagingLayer[NettyContext] with Sender with Logging {

  self: SiloSystem =>

  import logger._

  /* Promise the server is up and running. */
  protected def started: Promise[SiloSystem]

  /* Netty server constituents */
  private val server = new ServerBootstrap
  private val bossGrp = new NioEventLoopGroup
  private val wrkrGrp = new NioEventLoopGroup

  /* Latch to prevent server termination before `stop`
   * has been initiated by the surrounding silo system. */
  private val latch = new CountDownLatch(1)

  /** System message processing constituents
    *
    *      `forwarder --put-->  mq  <--pop-- receptor`
    *
    * `receptor` : Worker for all incoming messages from all channels.
    * `forwarder`: Server handler that forwards messages received by Netty.
    */
  private val mq = new LinkedBlockingQueue[NettyWrapper]
  private val receptor = new Receptor(mq)(ec, this, self)

  /* Not thread-safe, only accessed in the [[Receptor]].
   * Be careful, [[ConcurrentMap]] could be better than [[TrieMap]] */
  override lazy val statusFrom = initMessagingHub

  /* Thread-safe since it's accessed in the handlers */
  override lazy val unconfirmedResponses = TrieMap.empty[SiloSystemId, SelfDescribing]

  /* Thread-safe since it's accessed in the handlers */
  override lazy val silos = TrieMap.empty[SiloRefId, Silo[_]]

  /* Initialize a [[Netty http://goo.gl/0Z9pZM]]-based server.
   *
   * Note: [[NioEventLoopGroup]] is supposed to be used only for non-blocking
   * actions. Therefore we fork via [[EventExecutorGroup]] to pass system
   * messages to the `Receptor`.
   */
  trace("Server initializing...")

  server.group(bossGrp, wrkrGrp)
    .channel(classOf[NioServerSocketChannel])
    .childHandler(new ChannelInitializer[SocketChannel]() {
      override def initChannel(ch: SocketChannel): Unit = {
        val pipeline = ch.pipeline()
        pipeline.addLast(new Logger(LogLevel.TRACE))
        pipeline.addLast(new Encoder())
        pipeline.addLast(new Decoder())
        pipeline.addLast(new ServerHandler())
      }
    })
  // XXX are those options necessary?
  //.option(ChannelOption.SO_BACKLOG.asInstanceOf[ChannelOption[Any]], 128)
  //.childOption(ChannelOption.SO_KEEPALIVE.asInstanceOf[ChannelOption[Any]], true)

  trace("Server initializing done.")

  /* Forward incoming messages from Netty's event loop to internal
   * message queue processed by [[Receptor]].
   *
   * Be aware that due to the default constructor parameter of
   * [[io.netty.channel.SimpleChannelInboundHandler]] all messages will be
   * automatically released by passing them to
   * [[io.netty.util.ReferenceCountUtil#release(Object)]].
   */
  @ChannelHandler.Sharable
  private class ServerHandler() extends SimpleChannelInboundHandler[Message] with Logging {

    override def channelRead0(ctx: ChannelHandlerContext, msg: Message): Unit =
      mq add NettyWrapper(ctx, msg)

    override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
      //cause.printStackTrace()

      // XXX Don't just close the connection when an exception is raised.
      //     With the current setup of the channel pipeline, in order to respond
      //     with a raw text message --- recall only a
      //     silo sytem can create system messages due to `Id` --- additional
      //     encoder/ decoder are required.
      val msg = s"${cause.getClass.getSimpleName}: ${cause.getMessage}"
      logger.error(s"$msg\n$cause")
      ctx.close()

      //import ChannelFutureListener._
      //if (ctx.channel().isActive())
      //  ctx.writeAndFlush(msg).addListener(CLOSE)
      //else ()

    }

  }

  /** Start and bind server to accept incoming connections at port `at.port` */
  override def start(): Unit = try {
    trace("Server start...")

    executor execute receptor
    server bind(host.port) sync()
    started success self

    trace("Server start done.")
    info(s"Server listining at port ${host.port}.")

    new Thread {
      override def run(): Unit = latch.await()
    }.start()
  } catch { case e: Throwable => started failure e }

  /** Stop server.
    *
    * In Netty 4.0, you can just call `shutdownGracefully` on the
    * `EventLoopGroup` that manages all your channels. Then all ''existing
    * channels will be closed automatically'' and reconnection attempts should
    * be rejected.
    */
  override def stop(): Unit = {
    trace("Server stop...")

    receptor.stop()
    wrkrGrp.shutdownGracefully()
    bossGrp.shutdownGracefully()
    latch.countDown()

    trace("Server stop done.")
  }

}

