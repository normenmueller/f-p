package fp
package backend
package netty
package handlers

import fp.core.Materialized
import fp.model.{Populated, Populate, Message}
import io.netty.channel.ChannelHandlerContext

import scala.concurrent.{ExecutionContext, Future}
import scala.spores.SporePickler

/** Processes a concrete type of message asynchronously.
  *
  * The handlers define how a message should be processed based on its
  * message type. They are meant to avoid message processing in the thread
  * that receives the messages and, therefore, it avoids blocking it. This
  * is a more efficient approach once this receptor has to process a huge
  * amount of requests from other nodes in the network.
  */
trait Handler[T <: Message] {
  type NettyContext = ChannelHandlerContext
  def process(msg: T, ctx: NettyContext)
             (implicit server: Server, ec: ExecutionContext): Future[Unit]
}

object PopulateHandler extends Handler[Populate[_]] {
  import fp.model.SimplePicklingProtocol._
  import SporePickler._

  def process(msg: Populate[_], ctx: NettyContext)
             (implicit server: Server, ec: ExecutionContext) =
    Future {
      val newSilo = msg.gen()
      val refId = SiloRefId(server.host)
      server.silos += ((refId, newSilo))
      val nodeId = Materialized(refId)
      server.tell(ctx.channel, Populated(msg.id, nodeId))
    }
}


