package fp
package backend
package netty

import com.typesafe.scalalogging.{ StrictLogging => Logging }
import fp.model._

import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext.Implicits.{ global => executor }
import scala.concurrent.{ Future, Promise }
import scala.language.postfixOps
import scala.pickling.Pickler
import scala.util.Try

/** A Netty-based implementation of a silo system. */
class SiloSystem extends AnyRef with backend.SiloSystem
    with PicklingProtocol with Tell with Ask with Logging {

  import logger._
  import picklingProtocol._

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

  import _root_.io.netty.bootstrap.Bootstrap
  import _root_.io.netty.channel.nio.NioEventLoopGroup
  import _root_.io.netty.channel.socket.SocketChannel
  import _root_.io.netty.channel.socket.nio.NioSocketChannel
  import _root_.io.netty.channel.{ Channel, ChannelHandler }
  import _root_.io.netty.channel.{ ChannelHandlerContext, ChannelInitializer }
  import _root_.io.netty.channel.{ ChannelOption, SimpleChannelInboundHandler }
  import _root_.io.netty.handler.logging.{ LogLevel, LoggingHandler => Logger }

  import scala.collection._

  private val statusOf: mutable.Map[Host, Status] = new TrieMap[Host, Status]

  private def connect(to: Host): Future[Channel] = statusOf.get(to) match {
    case None => channel(to) flatMap {
      case ok: Connected =>
        statusOf += (to -> ok)
        Future.successful(ok.channel)
      case Disconnected =>
        /* TODO Think better alternative than reconnecting and
         * TODO If no alternative, set a policy of n tries. */
        info("Try to connect again after spurious exception...")
        statusOf -= to
        connect(to)
    }
    case Some(Disconnected) =>
      statusOf -= to
      connect(to)
    case Some(Connected(channel, _)) =>
      Future.successful(channel)
  }

  private def channel(to: Host): Future[Status] = {
    val wrkr = new NioEventLoopGroup
    Try {
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
    } recover {
      case t: Throwable =>
        wrkr.shutdownGracefully()
        error("Exception caught when processing message in the pipeline", t)
        Future.successful[Status](Disconnected)
    } get
  }

  /**
   * Handle all the incoming [[Message]]s that come through the Netty pipeline
   * and fulfilling an existing promise in case it matches the id of any message.
   */
  @ChannelHandler.Sharable
  private class ClientHandler() extends SimpleChannelInboundHandler[Message] with Logging {

    import logger._

    override def channelRead0(ctx: ChannelHandlerContext, msg: model.Message): Unit = {
      trace(s"Received message: $msg")

      msg match {
        case response: Response => promiseOf(response.id).success(response)
        case _ => warn(s"A response id doesn't match an expected promise: $msg")
      }
    }

    override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
      error("Exception caught in the `ClientHandler`", cause)
      ctx.close()
    }

  }

}

