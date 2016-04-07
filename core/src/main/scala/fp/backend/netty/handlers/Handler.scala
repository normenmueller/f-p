package fp
package backend
package netty
package handlers

import fp.util.RuntimeHelper

import scala.concurrent.{ExecutionContext, Future}
import scala.pickling.{PickleFormat, Pickler, Unpickler}

import fp.core.{FlatMap, Map, Materialized}
import fp.model._
import fp.model.PicklingProtocol._

/** Process a concrete type of message **asynchronously**.
  *
  * The handlers define how a message should be processed based on its
  * message type. They are meant to avoid message processing in the thread
  * that receives the messages and, therefore, it avoids blocking it. This
  * is a more efficient approach once this receptor has to process a huge
  * amount of requests from other nodes in the network.
  */
trait Handler[T <: Message] {
  def handle(msg: T, ctx: NettyContext)
            (implicit server: Server, system: SiloSystem, ec: ExecutionContext): Future[Unit]

  /** Update the pending message from which the system is
    * expecting an explicit confirmation from the sender and
    * send the reply to it.
    */
  def reply[R <: Response: Pickler: Unpickler](msg: R, ctx: NettyContext)
                                             (implicit server: Server) = {
    val wrapped = server.wrapBeforeSending(msg)
    server.unconfirmedResponses += (msg.senderId -> wrapped)
    server.sendAndForget(ctx.channel, wrapped)
  }

}

object PopulateHandler extends Handler[Populate[_]] {

  import fp.model.PicklingProtocol._

  def handle(msg: Populate[_], ctx: NettyContext)
            (implicit server: Server, system: SiloSystem, ec: ExecutionContext) = {
    Future[Unit] {
      val refId = SiloRefId(server.host)
      server.silos += ((refId, msg.gen(())))
      val node = Materialized(refId)
      val response = Populated(msg.id, system.systemId, node)
      reply(response, ctx)
    }
  }

}

object TransformHandler extends Handler[Transform] {

  def handle(msg: Transform, ctx: NettyContext)
            (implicit server: Server, system: SiloSystem, ec: ExecutionContext) = {
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

      import RuntimeHelper.getInstance
      val (pcn, ucn) = (msg.picklerClassName, msg.unpicklerClassName)
      val pickler = getInstance[Pickler[Transformed[Any]]](pcn)
      val unpickler = getInstance[Unpickler[Transformed[Any]]](ucn)
      val response = Transformed[Any](msg.id, system.systemId, newSilo.data)

      reply(response, ctx)(pickler, unpickler, server)
    }
  }

}

