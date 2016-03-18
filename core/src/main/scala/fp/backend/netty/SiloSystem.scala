package fp
package backend
package netty

import com.typesafe.scalalogging.{ StrictLogging => Logging }
import fp.model._
import fp.util.AsyncExecution

import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext.Implicits.{ global => executor }
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.language.postfixOps
import scala.pickling.Pickler
import scala.spores.SporePickler
import scala.util.Try

import io.netty.bootstrap.Bootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.channel.{ Channel, ChannelHandler }
import io.netty.channel.{ ChannelHandlerContext, ChannelInitializer }
import io.netty.channel.{ ChannelOption, SimpleChannelInboundHandler }
import io.netty.handler.logging.{ LogLevel, LoggingHandler => Logger }

/** A Netty-based implementation of a silo system. */
class SiloSystem(implicit val ec: ExecutionContext) extends backend.SiloSystem
    with Ask with Tell with AsyncExecution with Logging {

  import logger._
  import SimplePicklingProtocol._
  import SporePickler._

  override val name = java.util.UUID.randomUUID.toString

  override val responsesFor = new TrieMap[MsgId, Promise[Response]]

  override def request[R <: ClientRequest: Pickler]
    (at: Host)(request: MsgId => R): Future[Response] =
      connect(at) flatMap { via => ask(via, request(MsgIdGen.next)) }

  override def terminate(): Future[Unit] = {
    val promise = Promise[Unit]

    info(s"Silo system `$name` terminating...")
    val to = statusOf collect {
      case (host, Connected(channel, worker)) =>
        trace(s"Closing connection to `$host`.")
        tell(channel, Disconnect(MsgIdGen.next))
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
        case response: Response => responsesFor(response.id).success(response)
        case _ => warn(s"A response id doesn't match an expected promise: $msg")
      }
    }

    override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
      error("Exception caught in the `ClientHandler`", cause)
      ctx.close()
    }

  }

}

/** Provides a set of operations needed to create [[SiloSystem]]s. */
object SiloSystem extends SiloSystemCompanion {

  /** Return a [[SiloSystem]] running in server mode if port is not [[None]].
    * Otherwise, return a [[SiloSystem]] in client mode.
    *
    * @param port Port number
    */
  override def apply(port: Option[Int] = None): Future[SiloSystem] = {
    port match {
      case Some(portNumber) => apply(Host("127.0.0.1", portNumber))
      case None => Future.successful(new SiloSystem)
    }
  }

  /** Return a server-based silo system.
    *
    * A silo system running in server mode has an underlying [[fp.backend.Server]]
    * to host silos and make those available to other silo systems.
    *
    * The underlying server is private to the silo system, i.e., only the silo
    * system itself directly communicates with the server. A user/client only
    * directly communicates with such a silo system.
    *
    * @param atHost Host where the server will be booted up
    */
  def apply(atHost: Host): Future[SiloSystem] = {
    val promise = Promise[SiloSystem]
    val withServer = new SiloSystem with Server {
      override val host = atHost
      override val name = atHost.toString
      override val started = promise
    }
    promise.future
  }

}

