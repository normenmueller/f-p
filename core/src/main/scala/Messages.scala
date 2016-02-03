package silt

sealed trait Identifiable {

  val id: MsgId

}

// F-P internal system messages

sealed abstract class Message

sealed abstract class Request extends Message
case object Disconnect extends Request
case object Terminate  extends Request

sealed abstract class RSVP extends Request with Identifiable
case class Populate[T](id: MsgId, fac: () => Silo[T]) extends RSVP 
case class Traverse(id: MsgId, node: graph.Node) extends RSVP

sealed abstract class Response extends Message with Identifiable
case class Populated(id: MsgId, ref: RefId) extends Response
case class Traversed(id: MsgId, data: Any) extends Response

// vim: set tw=120 ft=scala:
