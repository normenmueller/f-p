package silt

import silt.core.Node

sealed trait Identifiable {
  val id: MsgId
}

sealed abstract class Message

sealed abstract class Request extends Message
case object Disconnect extends Request
case object Terminate extends Request

sealed abstract class RSVP extends Request with Identifiable
case class Populate[T](id: MsgId, factory: () => Silo[T]) extends RSVP
case class Traverse(id: MsgId, node: Node) extends RSVP

sealed abstract class Response extends Message with Identifiable
case class Populated(id: MsgId, ref: RefId) extends Response
case class Traversed(id: MsgId, data: Any) extends Response

