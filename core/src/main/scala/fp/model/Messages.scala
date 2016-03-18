package fp
package model

import fp.SiloFactory.SiloGen
import fp.core.{Materialized, Node}

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

/** We use path-dependent types to overcome some difficulties when
  * generating picklers for [[Response]]s which actual types we don't know
  * in some given scenarios, e.g. they are stored in a `Set[Response]`.
  * See possible use case in the netty `Receptor`.
  */
@directSubclasses(Array(classOf[Populated], classOf[Transformed[_]]))
sealed abstract class Response extends Message {
  type B <: Response

  def specialize: B
  def getPickler: Pickler[B]
}

case class Populated(id: MsgId, node: Materialized) extends Response {
  override type B = Populated

  override def specialize: Populated =
    this.asInstanceOf[Populated]

  import PicklingProtocol._

  override def getPickler: Pickler[Populated] =
    implicitly[Pickler[Populated]]
}

case class Transformed[T: Pickler](id: MsgId, data: T)
                                  (implicit p: Pickler[Transformed[T]]) extends Response {
  override type B = Transformed[T]

  override def specialize: Transformed[T] =
    this.asInstanceOf[Transformed[T]]

  /* We can't ask for a generated `Pickler` for `Transformed[T]`
   * because T here is not detected as a `Class` but as a `Type`.
   * Then, we force the call site to generate this for us and reuse it. */
  override def getPickler: Pickler[Transformed[T]] = p
}

