package silt

import java.util.concurrent.CancellationException

import scala.concurrent.{ ExecutionContext, Promise, Future }
import scala.language.implicitConversions
import scala.util.Try

import io.netty.channel.{ Channel, ChannelFuture, ChannelFutureListener, ChannelHandlerContext }
import io.netty.handler.logging.{ LogLevel, LoggingHandler => Logger }

package object impl {

  // Note: [[SystemMessageDecoder]] MUST NOT be a @Sharable handler!
  private[impl] def decoder = new SystemMessageDecoder()
  private[impl] val ENCODER = new SystemMessageEncoder()
  private[impl] val FORWARDER = new Forwarder( /* XXX processor */ )
  private[impl] val LOGGER = new Logger(LogLevel.TRACE)
  // XXX new LengthFieldBasedFrameDecoder ?
  // XXX new ChunkedWriteHandler() ?

  // Connection status

  import io.netty.channel.{ Channel, EventLoopGroup }

  private[impl] sealed abstract class Status
  private[impl] case class Connected(channel: Channel, worker: EventLoopGroup) extends Status
  private[impl] case object Disconnected extends Status

  // Messages

  private[impl] sealed abstract class Message
  private[impl] case class Incoming(ctx: ChannelHandlerContext, msg: Any) extends Message

  // Utilities

  implicit def nettyFutureToScalaFuture(future: ChannelFuture): Future[Channel] = {
    val p = Promise[Channel]()
    future.addListener(new ChannelFutureListener {
      def operationComplete(future: ChannelFuture): Unit =
        p complete Try(
          if (future.isSuccess) future.channel
          else if (future.isCancelled) throw new CancellationException
          else throw future.cause
        )
    })
    p.future
  }

}

// vim: set tw=80 ft=scala:
