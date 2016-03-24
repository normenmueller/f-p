package fp
package backend

import java.net.InetSocketAddress
import java.util.concurrent.CancellationException

import fp.model.Message
import io.netty.channel._

import scala.concurrent.{Future, Promise}
import scala.language.implicitConversions
import scala.util.Try

package object netty {

  type NettyContext = ChannelHandlerContext

  /**
   * Wrapper around any message of the internal function-passing protocol that
   * stores a [[ChannelHandlerContext]] to give further information to Netty.
   *
   * @param ctx Netty context
   * @param msg Function-passing model
   */
  private[netty] final case class NettyWrapper(ctx: NettyContext, msg: Message)
    extends WrappedMsg[NettyContext] with Comparable[NettyWrapper] {
      /* Ugly because we use java PriorityBlockingQueue and requires Comparable */
      def compareTo(m2: NettyWrapper): Int =
        Ordering.Int.compare(m2.msg.id.value, msg.id.value)
    }

  /**
   * Describe a connection between two nodes in a given network. It can be
   * either [[Connected]] or [[Disconnected]].
   */
  private[netty] sealed abstract class Status
  private[netty] final case class Connected(channel: Channel, worker: EventLoopGroup) extends Status
  private[netty] case object Disconnected extends Status

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

  /** Enrich a NettyContext and add explicit method to get the remote host. */
  implicit class EnrichedContext(ctx: NettyContext) {
    def getRemoteHost: InetSocketAddress =
      ctx.channel.remoteAddress.asInstanceOf[InetSocketAddress]
  }

}

