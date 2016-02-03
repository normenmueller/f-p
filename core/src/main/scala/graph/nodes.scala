package silt
package graph

//import scala.spores._
//import scala.pickling._
//import Defaults._

/**
  * A node in the computation graph.
  *
  * We must be able to generate a pickler for it.
  */
sealed abstract class Node {

  def id: SiloRefId

}

final case class Materialized(id: SiloRefId) extends Node

// XXX final case class Apply[U, T <: Traversable[U], V, S <: Traversable[V]](input: Node, refId: Int, fun: T => S, pickler: Pickler[Spore[T, S]], unpickler: Unpickler[Spore[T, S]]) extends Node

// XXX final case class FMapped[U, T <: Traversable[U], V, S <: Traversable[V]](input: Node, refId: Int, fun: T => SiloRef[V, S], pickler: Pickler[Spore[T, SiloRef[V, S]]], unpickler: Unpickler[Spore[T, SiloRef[V, S]]]) extends Node

// XXX final case class MultiInput[R](inputs: Seq[PumpNodeInput[_, _, R, _]], refId: Int, destHost: Host, emitterId: Int) extends Node

// XXX final case class PumpNodeInput[U, V, R, P](from: Node, fromHost: Host, fun: P, pickler: Pickler[P], unpickler: Unpickler[P], bf: BuilderFactory[V, R])

// XXX sealed abstract class Command

// remote message
// XXX case class DoPumpTo[A, B, P](node: Node, fun: P, pickler: Pickler[P], unpickler: Unpickler[P], emitterId: Int, destHost: Host, destRefId: Int) extends Command

// XXX case class CommandEnvelope(cmd: Command)

// vim: set tw=120 ft=scala:
