package silt

sealed trait Identifiable {

  val id: Id

}

sealed abstract class Message

sealed abstract class Request extends Message with Identifiable

sealed abstract class Response extends Message with Identifiable

case class Populate[T](id: Id, fac: SiloFactory[T]) extends Request
case class Populated(id: Id, cor: Id) extends Response

case object Disconnect extends Message
case object Terminate extends Message

// vim: set tw=80 ft=scala:
