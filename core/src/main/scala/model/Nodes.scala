package fp
package core

import scala.pickling._

/** Represents a serializable entity using picklers/unpicklers */
trait Serializable[T] {
  def pickler: Pickler[T]
  def unpickler: Unpickler[T]
}

/** A node in the computation graph.
  *
  * Mix in with [[Serializable]] so that the nodes can be sent
  * over the wire.
  */
@directSubclasses(Array(classOf[Materialized]))
sealed abstract class Node {
  def refId: RefId
}

final case class Materialized(refId: RefId) extends Node

/*

final case class Apply[U, T <: Traversable[U], V, S <: Traversable[V]](
  input: Node, refId: Int, f: T => S
) extends Node with Serializable[Spore[T, S]]

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
