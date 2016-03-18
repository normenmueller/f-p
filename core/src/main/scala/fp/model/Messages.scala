package fp
package model

import fp.SiloFactory.SiloGen
import fp.core.{Materialized, NodeId, Node}

import scala.spores._
import scala.pickling.{Pickler, directSubclasses}

/** A unique silo system message identifier. */
final case class MsgId private[fp](value: Int) extends AnyVal


/** A message is any packet of information exchanged among the nodes of
  * a network that complies with the function-passing protocol.
  */
@directSubclasses(Array(classOf[Request], classOf[Response]))
sealed abstract class Message {
  val id: MsgId
}

@directSubclasses(Array(classOf[ClientRequest]))
sealed abstract class Request extends Message

case class Disconnect(id: MsgId) extends Request

case class Terminate(id: MsgId) extends Request

/** [[RVSP]] is the short for "Respondez s'il vous plaît", which means that
  * the sender is expecting a response from the recipient.
  */
sealed trait RVSP

@directSubclasses(Array(classOf[Populate[_]], classOf[Transform]))
sealed abstract class ClientRequest extends Request

case class Populate[T](id: MsgId, gen: SiloGen[T]) extends ClientRequest with RVSP

case class Transform(id: MsgId, node: Node) extends ClientRequest with RVSP

@directSubclasses(Array(classOf[Populated], classOf[Transformed[_]]))
sealed abstract class Response extends Message

case class Populated(id: MsgId, node: Materialized) extends Response

case class Transformed[T](id: MsgId, data: T) extends Response

