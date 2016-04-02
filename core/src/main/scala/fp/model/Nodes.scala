package fp
package core

import fp.util.{IntGen, Gen}

import scala.pickling._
import scala.spores.Spore

/** Represents a serializable entity using picklers/unpicklers */
trait Serializable[T] {
  def pickler: Pickler[T]
  def unpickler: Unpickler[T]
}

/** A unique silo system reference identifier.
  * This should be `AnyVal` but scala-pickling does not handle
  * value classes correctly so it'll be added in the future */
private[fp] final case class NodeId(value: String)

object NodeIdGen extends Gen[NodeId] {
  /* We may want to add a host-related prefix to `RefId` */
  object IntGen extends IntGen
  override def next: NodeId = NodeId(IntGen.next.toString)
}

/** A node in the computation graph.
  *
  * It is either `Materialized` or a `Transformation`.
  *
  * Mix in with [[Serializable]] so that the nodes can be sent
  * over the wire.
  */
@directSubclasses(Array(classOf[Materialized], classOf[Map[_, _]], classOf[FlatMap[_, _]]))
sealed abstract class Node {
  def nodeId: NodeId
}

final case class Materialized(
  refId: SiloRefId,
  nodeId: NodeId = NodeIdGen.next
) extends Node

@directSubclasses(Array(classOf[Map[_, _]], classOf[FlatMap[_, _]]))
sealed abstract class Transformation extends Node {
  def target: Node
}

final case class Map[T, S](
  target: Node,
  f: Spore[T, S],
  nodeId: NodeId = NodeIdGen.next
)(implicit p: Pickler[Spore[T, S]], u: Unpickler[Spore[T, S]]) extends Transformation

final case class FlatMap[T, S](
  target: Node,
  f: Spore[T, SiloRef[S]],
  nodeId: NodeId = NodeIdGen.next
)(implicit p: Pickler[Spore[T, SiloRef[S]]], u: Unpickler[Spore[T, SiloRef[S]]]) extends Transformation

object NodesPicklerUnpicklers {
  /*
  implicit object NodePicklerUnpickler extends Pickler[Node] with Unpickler[Node] {

    override def tag = implicitly[FastTypeTag[Node]]

    override def pickle(picklee: Node, builder: PBuilder): Unit = ???


    override def unpickle(tag: String, reader: PReader): Any = ???
  }
  */
}

/*

final case class FMapped[U, T <: Traversable[U], V, S <: Traversable[V]](
  input: Node, refId: Int, f: T => SiloRef[V, S]
) extends Node with Serializable[Spore[T, SiloRef[V, S]]]

final case class MultiInput[R](
  inputs: Seq[PumpNodeInput[_, _, R, _]], refId: Int, dest: Host, emitterId: Int
) extends Node // This should extend Serializable

final case class PumpNodeInput[U, V, R, P](
  from: Node, fromHost: Host, fun: P, bf: BuilderFactory[V, R]
) extends Node with Serializable[P]
*/

/** Directed Acyclic Graph (DAG) that represents several transformation over
  * some data stored in a [[Silo]]. This is known as the [[Lineage]] which is
  * sent to other [[Host]]s to model computations from the initial data
  */
final case class Lineage(node: Node)
