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
@directSubclasses(Array(classOf[Materialized], classOf[Transformation[_, _]]))
sealed abstract class Node {

  def nodeId: NodeId

  @scala.annotation.tailrec
  final def findClosestMaterialized: Materialized = {
    this match {
      case m: Materialized => m
      case t: Transformation[u, s] =>t.target.findClosestMaterialized
    }
  }

}

final case class Materialized(
  refId: SiloRefId,
  nodeId: NodeId = NodeIdGen.next
) extends Node

@directSubclasses(Array(classOf[Map[_, _]], classOf[FlatMap[_, _]]))
sealed abstract class Transformation[T, S] extends Node {
  def target: Node
  def f: Spore[T, S]
}

final case class Map[T, S](
  target: Node,
  f: Spore[T, S],
  nodeId: NodeId = NodeIdGen.next
)(implicit val ftt: FastTypeTag[T], val fts: FastTypeTag[S]) extends Transformation[T, S]

final case class FlatMap[T: FastTypeTag, S: FastTypeTag](
  target: Node,
  f: Spore[T, Silo[S]],
  nodeId: NodeId = NodeIdGen.next
)(implicit val ftt: FastTypeTag[T], val fts: FastTypeTag[S]) extends Transformation[T, Silo[S]]



/** Directed Acyclic Graph (DAG) that represents several transformation over
  * some data stored in a [[Silo]]. This is known as the [[Lineage]] which is
  * sent to other [[Host]]s to model computations from the initial data
  */
final case class Lineage(node: Node)

