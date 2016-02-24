package fp
package model

import fp.core.Node
import scala.pickling.directSubclasses

sealed trait Identifiable {
  val id: MsgId
}

/** A message is any packet of information exchanged among the nodes of
  * a network that complies with the function-passing protocol.
  */
@directSubclasses(Array(classOf[Request], classOf[Response]))
sealed abstract class Message

@directSubclasses(Array(classOf[ClientRequest]))
sealed abstract class Request extends Message
case object Disconnect extends Request
case object Terminate extends Request

/** [[RVSP]] is the short for "Respondez s'il vous plaît", which means that
  * the sender is expecting a response from the recipient.
  */
sealed trait RVSP

@directSubclasses(Array(classOf[Populate[_]], classOf[Traverse]))
sealed abstract class ClientRequest extends Request with Identifiable

case class Populate[T](id: MsgId, factory: () => Silo[T])
  extends ClientRequest with RVSP

case class Traverse(id: MsgId, node: Node)
  extends ClientRequest with RVSP

@directSubclasses(Array(classOf[Populated], classOf[Traversed]))
sealed abstract class Response extends Message with Identifiable
case class Populated(id: MsgId, ref: RefId) extends Response
case class Traversed(id: MsgId, data: Any) extends Response

