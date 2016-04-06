package fp
package backend
package netty
package handlers

import fp.core.{FlatMap, Map, Materialized}
import fp.model._

import scala.concurrent.{ExecutionContext, Future}

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

  /** Updates the pending message from which the system is
    * expecting an explicit confirmation from the sender.
    */
  private[handlers] def storeAsPending[M <: Response]
      (res: M, ctx: NettyContext)(implicit server: Server): Unit =
    server.unconfirmedResponses += (ctx.getRemoteHost -> res)

}

object PopulateHandler extends Handler[Populate[_]] {

  import fp.model.PicklingProtocol._

  def handle(msg: Populate[_], ctx: NettyContext)
            (implicit server: Server, ec: ExecutionContext) = {
    Future[Unit] {
      val refId = SiloRefId(server.host)
      server.silos += ((refId, msg.gen(())))
      val nodeId = Materialized(refId)
      val response = Populated(msg.id, nodeId)
      storeAsPending(response, ctx)
      server.tell(ctx.channel, response)
    }
  }

}

object TransformHandler extends Handler[Transform] {

  import fp.model.PicklingProtocol._

  def handle(msg: Transform, ctx: NettyContext)
            (implicit server: Server, ec: ExecutionContext) = {
    Future[Unit] {

      val (id, node) = (msg.id, msg.node)
      val from = node.findClosestMaterialized

      val silo = server.silos(from.refId)
      val newSilo = node match {
        case m: Map[Any,Any] => silo.map(m.f)
        case fm: FlatMap[Any,Any] => silo.flatMap(fm.f)
      }

      val newSiloRefId = SiloRefId(server.host)
      server.silos += (newSiloRefId -> newSilo)

      println("received")
    }
  }

}

