package silt
package impl

import java.util.concurrent.CancellationException

import scala.concurrent.{ ExecutionContext, Promise, Future }
import scala.language.implicitConversions
import scala.util.Try

import io.netty.channel.{ Channel, ChannelHandlerContext, ChannelFuture, ChannelFutureListener, EventLoopGroup }

package object netty {

  // F-P internal system message enriched by Netty-specific context

  private[netty] final case class Message(ctx: ChannelHandlerContext, msg: silt.Message)

  // Connection status

  private[netty] sealed abstract class Status
  private[netty] case class Connected(channel: Channel, worker: EventLoopGroup) extends Status
  private[netty] case object Disconnected extends Status

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

// vim: set tw=120 ft=scala:
