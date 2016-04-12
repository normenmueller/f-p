package fp
package model

import fp.SiloFactory.SiloGen
import fp.core.{Materialized, Node}
import fp.util.UUIDGen

import scala.pickling.directSubclasses

/** A unique silo system message identifier.
  *
  * This can't be a value class since scala-pickling does not
  * deal with value classes properly when pickling/unpickling.
  * See [[https://github.com/scala/pickling/issues/391 this]].
  *
  * This should be a value class but scala pickling does not handle
  * value classes correctly so it will be added in the future.
  */
final case class MsgId(value: Int) {
  def increaseByOne = MsgId(value + 1)
}

final case class SiloSystemId(value: String = UUIDGen.next)

/** A message is any packet of information exchanged among the nodes of
  * a network that complies with the function-passing protocol.
  */
@directSubclasses(Array(classOf[Request], classOf[Response]))
sealed abstract class Message {
  val id: MsgId
  val senderId: SiloSystemId
}

@directSubclasses(Array(classOf[ClientRequest]))
sealed abstract class Request extends Message

case class Disconnect(id: MsgId, senderId: SiloSystemId) extends Request

case class Terminate(id: MsgId, senderId: SiloSystemId) extends Request

/** The sender expects a reply from the recipient. */
sealed trait ExpectsResponse

@directSubclasses(Array(classOf[Populate[_]], classOf[Transform]))
sealed abstract class ClientRequest extends Request with ExpectsResponse

case class Populate[T](id: MsgId, senderId: SiloSystemId, gen: SiloGen[T])
  extends ClientRequest

case class RequestData(id: MsgId, senderId: SiloSystemId,
                       node: Node) extends ClientRequest

case class Transform(id: MsgId, senderId: SiloSystemId, node: Node,
                     picklerClassName: String,
                     unpicklerClassName: String) extends ClientRequest

@directSubclasses(Array(classOf[Populated], classOf[Transformed[_]]))
sealed abstract class Response extends Message

case class Populated(id: MsgId, senderId: SiloSystemId,
                     node: Materialized) extends Response

/** Represents a successful transformation over some data of a [[Silo]] that
  * it's sent back to the node that requested it. It's the natural response
  * to a `send` operation since it returns the result of the transformation.
  */
case class Transformed[T](id: MsgId, senderId: SiloSystemId,
                          data: T) extends Response

