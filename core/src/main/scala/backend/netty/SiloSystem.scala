package fp
package backend
package netty

import fp.model.{ Disconnect, Response, ClientRequest, Message }

import scala.collection.concurrent.TrieMap
import scala.concurrent.{ ExecutionContext, Future, Promise }
import ExecutionContext.Implicits.{ global => executor }
import scala.pickling._
import Defaults._

import com.typesafe.scalalogging.{ StrictLogging => Logging }

/** A Netty-based implementation of a silo system. */
class SiloSystem extends AnyRef with backend.SiloSystem with Tell with Ask with Logging {

  import logger._

  override val name = java.util.UUID.randomUUID.toString

  override val promiseOf = new TrieMap[MsgId, Promise[Response]]

  override def request[R <: ClientRequest: Pickler](at: Host)(request: MsgId => R): Future[Response] =
    connect(at) flatMap { via => ask(via, request(MsgIdGen.next)) }

  override def withServer(host: Host): Future[fp.SiloSystem] = {
    val promise = Promise[fp.SiloSystem]

    executor execute new SiloSystem with Server {
      override val at = host
      override val name = host.toString
      override val started = promise
    }

    promise.future
  }

  override def terminate(): Future[Unit] = {
    val promise = Promise[Unit]

    info(s"Silo system `$name` terminating...")
    val to = statusOf collect {
      case (host, Connected(channel, worker)) =>
        trace(s"Closing connection to `$host`.")
        tell(channel, Disconnect)
          .andThen { case _ => worker.shutdownGracefully() }
          .andThen { case _ => statusOf += (host -> Disconnected) }
    }

    // Close connections FROM other silo systems
    // val from = XXX

    // Terminate underlying server
    Future.sequence(to) onComplete {
      case _ =>
        promise success (this match {
          case server: Server => server.stop()
          case _ => ()
        })
        info(s"Silo system `$name` terminating done.")
    }

    promise.future
  }

  // ----

  import scala.collection._
  import _root_.io.netty.bootstrap.Bootstrap
  import _root_.io.netty.channel.{ Channel, ChannelHandler, ChannelHandlerContext }
  import _root_.io.netty.channel.{ ChannelInitializer, ChannelOption, SimpleChannelInboundHandler }
  import _root_.io.netty.channel.nio.NioEventLoopGroup
  import _root_.io.netty.channel.socket.SocketChannel
  import _root_.io.netty.channel.socket.nio.NioSocketChannel
  import _root_.io.netty.handler.logging.{ LogLevel, LoggingHandler => Logger }

  private val statusOf: mutable.Map[Host, Status] = new TrieMap[Host, Status]

  private def connect(to: Host): Future[Channel] = statusOf.get(to) match {
    case None => channel(to) map { status =>
      statusOf += (to -> status)
      status.channel
    }
    case Some(Disconnected) =>
      statusOf -= to
      connect(to)
    case Some(Connected(channel, _)) =>
      Future.successful(channel)
  }

  private def channel(to: Host): Future[Connected] = {
    val wrkr = new NioEventLoopGroup
    try {
      val b = new Bootstrap
      b.group(wrkr)
        .channel(classOf[NioSocketChannel])
        .handler(new ChannelInitializer[SocketChannel] {
          override def initChannel(ch: SocketChannel): Unit = {
            val pipeline = ch.pipeline()
            pipeline.addLast(new Logger(LogLevel.TRACE))
            pipeline.addLast(new Encoder())
            pipeline.addLast(new Decoder())
            pipeline.addLast(new ClientHandler())
          }
        })
        .option(ChannelOption.SO_KEEPALIVE.asInstanceOf[ChannelOption[Any]], true)
        .connect(to.address, to.port).map(Connected(_, wrkr))
    } catch {
      case t: Throwable =>
        wrkr.shutdownGracefully()
        throw t
    }
  }

  @ChannelHandler.Sharable
  private class ClientHandler() extends SimpleChannelInboundHandler[Message] with Logging {

    import logger._

    override def channelRead0(ctx: ChannelHandlerContext, msg: model.Message): Unit = {
      trace(s"Received message: $msg")

      // response to request, so look up promise
      msg match {
        case theMsg: Response => promiseOf(theMsg.id).success(theMsg)
        case _ => /* do nothing */
      }
    }

    override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
      cause.printStackTrace()
      ctx.close()
    }

  }

}

