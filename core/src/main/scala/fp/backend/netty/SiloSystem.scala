package fp
package backend
package netty

import com.typesafe.scalalogging.{StrictLogging => Logging}

import fp.model._
import fp.util.{UUIDGen, AsyncExecution}

import scala.collection.mutable.{Map => Mmap}
import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext.Implicits.{global => executor}
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.language.postfixOps
import scala.pickling.{Pickler, Unpickler}
import scala.util.{Failure, Success, Try}

import io.netty.bootstrap.Bootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.channel.{Channel, ChannelHandler}
import io.netty.channel.{ChannelHandlerContext, ChannelInitializer}
import io.netty.channel.{ChannelOption, SimpleChannelInboundHandler}
import io.netty.handler.logging.{LogLevel, LoggingHandler => Logger}

/** A Netty-based implementation of a silo system. */
class SiloSystem(implicit val ec: ExecutionContext) extends backend.SiloSystem
  with Ask with Tell with AsyncExecution with Logging { self =>

  import logger._
  import PicklingProtocol._

  override val name = UUIDGen.next

  override val responsesFor = new TrieMap[MsgId, Promise[Response]]

  override def request[R <: ClientRequest : Pickler: Unpickler]
    (to: Host)(request: MsgId => R): Future[Response] =
      connect(to) flatMap { via => ask(via, request(MsgIdGen.next)) }

  private def shutdownIfServer() = {
    self match {
      case s: Server => s.stop()
      case _ => ()
    }
  }

  override def terminate(): Future[Unit] = {
    val promise = Promise[Unit]

    info(s"Silo system `$name` terminating...")
    val closeAliveConnections = Future {
      stateOf collect {
        case (host, Connected(channel, worker)) =>
          trace(s"Closing connection to `$host`.")
          tell(channel, Disconnect(MsgIdGen.next))
            .andThen { case _ => worker.shutdownGracefully() }
            .andThen { case _ => stateOf += (host -> Disconnected) }
      }
    }

    closeAliveConnections onComplete {
      case s: Success[_] =>
        promise.success(shutdownIfServer())
        info(s"Silo system `$name` has been terminated.")
      case f: Failure[_] =>
        promise.success(shutdownIfServer())
        error(s"Error when terminating the Silo system `$name`:\n$f")
    }

    promise.future
  }

  private val stateOf: Mmap[Host, State] = new TrieMap[Host, State]

  private def connect(to: Host): Future[Channel] = {
    stateOf.get(to) match {
      case None => channel(to) flatMap {
        case ok: Connected =>
          Future.successful(ok.channel)
        case Disconnected =>
          /* TODO Think better alternative than reconnecting and
           * TODO If no alternative, set a policy of n tries. */
          info("Try to connect again after spurious exception...")
          stateOf -= to
          connect(to)
      }

      case Some(Disconnected) =>
        stateOf -= to
        connect(to)

      case Some(Connected(channel, _)) =>
        Future.successful(channel)
    }
  }

  private def channel(to: Host): Future[State] = {
    val wrkr = new NioEventLoopGroup
    Try {
      (new Bootstrap).group(wrkr)
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
        Future.successful[State](Disconnected)
    } get
  }

  /** Handle all the incoming [[Message]]s that come through the Netty pipeline
    * and fulfilling an existing promise in case it matches the id of any message.
    */
  @ChannelHandler.Sharable
  private class ClientHandler() extends SimpleChannelInboundHandler[Message]
      with Logging {

    import logger._

    override def channelRead0(ctx: ChannelHandlerContext,
                              msg: Message): Unit = {
      trace(s"Received message: $msg")
      msg match {
        case r: Response => responsesFor(r.id).success(r)
        case _ => warn(s"A response id doesn't match an expected promise: $msg")
      }
    }

    override def exceptionCaught(ctx: ChannelHandlerContext,
                                 cause: Throwable): Unit = {
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
      case Some(portNumber) =>
        apply(Host("127.0.0.1", portNumber))
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
    * @param at Host where the server will be booted up
    */
  def apply(at: Host): Future[SiloSystem] = {
    val promise = Promise[SiloSystem]

    executor execute {
      new SiloSystem with Server {
        override val host = at
        override val name = at.toString
        override val started = promise
      }
    }

    promise.future
  }

}

