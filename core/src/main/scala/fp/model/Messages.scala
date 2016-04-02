package fp
package model

import fp.SiloFactory.SiloGen
import fp.core.{Materialized, Node}

import scala.reflect.ClassTag
import scala.spores._
import scala.pickling._
import PicklingProtocol._
import sporesPicklers._

/** A unique silo system message identifier.
  *
  * This can't be a value class since scala-pickling does not
  * deal with value classes properly when pickling/unpickling.
  * See [[https://github.com/scala/pickling/issues/391 this]].
  *
  * This should be a value class but scala pickling does not handle
  * value classes correctly so it will be added in the future.
  */
final case class MsgId(value: Int)


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
  * generating picklers for [[Response]]s whose actual types we don't know
  * in some given scenarios, e.g. they are stored in a `Set[Response]`.
  * See possible use case in the netty `Receptor`.
  */
@directSubclasses(Array(classOf[Populated], classOf[Transformed[_]]))
sealed abstract class Response extends Message {

  type Id <: Response

  def specialize: Id
  def getPickler: Pickler[Id]
  def getUnpickler: Unpickler[Id]

}

case class Populated(id: MsgId, node: Materialized) extends Response {

  override type Id = Populated

  override def specialize: Populated =
    this.asInstanceOf[Populated]

  import PicklingProtocol._
  override def getPickler: Pickler[Populated] =
    implicitly[Pickler[Populated]]

  override def getUnpickler: Unpickler[Populated] =
    implicitly[Unpickler[Populated]]

}

case class Transformed[T: Pickler: Unpickler: ClassTag](id: MsgId, data: T) extends Response {

  override type Id = Transformed[T]

  override def specialize: Transformed[T] =
    this.asInstanceOf[Transformed[T]]

  /* We can't ask for a generated `Pickler` for `Transformed[T]`
   * because T here is not detected as a `Class` but as a `Type`.
   * Then, we force the call site to generate this for us and reuse it. */
  override def getPickler: Pickler[Transformed[T]] =
    implicitly[Pickler[Transformed[T]]]

  /* We can't ask for a generated `Pickler` for `Transformed[T]`
   * because T here is not detected as a `Class` but as a `Type`.
   * Then, we force the call site to generate this for us and reuse it. */
  override def getUnpickler: Unpickler[Transformed[T]] =
    implicitly[Unpickler[Transformed[T]]]

}

