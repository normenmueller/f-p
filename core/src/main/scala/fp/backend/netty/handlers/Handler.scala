package fp
package backend
package netty
package handlers

import fp.core.Materialized
import fp.model.{Response, Populated, Populate, Message}

import scala.concurrent.{ExecutionContext, Future}
import scala.spores.SporePickler

/** Processes a concrete type of message **asynchronously**.
  *
  * The handlers define how a message should be processed based on its
  * message type. They are meant to avoid message processing in the thread
  * that receives the messages and, therefore, it avoids blocking it. This
  * is a more efficient approach once this receptor has to process a huge
  * amount of requests from other nodes in the network.
  */
trait Handler[T <: Message] {
  def handle(msg: T, ctx: NettyContext)
            (implicit server: Server, ec: ExecutionContext): Future[Unit]

  /** Updates the last pending message that expected confirmation from the
    * client. It removes the previous pending message. */
  def storeAsPending[M <: Response](res: M, ctx: NettyContext)
                                   (implicit server: Server): Unit = {
    server.pendingOfConfirmation += (ctx.getRemoteHost -> res)
  }
}

object PopulateHandler extends Handler[Populate[_]] {

  import fp.model.SimplePicklingProtocol._
  import SporePickler._

  def handle(msg: Populate[_], ctx: NettyContext)
            (implicit server: Server, ec: ExecutionContext) =
    Future[Unit] {
      val refId = SiloRefId(server.host)
      server.silos += ((refId, msg.gen()))
      val nodeId = Materialized(refId)
      val response = Populated(msg.id, nodeId)
      storeAsPending(response, ctx)
      server.tell(ctx.channel, response)
    }

}

