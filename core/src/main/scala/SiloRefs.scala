package silt

import scala.concurrent.Future
import scala.pickling.{ Pickler, Unpickler }

import com.typesafe.scalalogging.{ StrictLogging => Logging }

/** Immutable and serializable handle to a silo.
  *
  * The referenced silo may or may not reside on the local host or inside the same silo system. A [[SiloRef]] can be
  * obtained from [[SiloRefFactory]], an interface which is implemented by [[SiloSystem]].
  *
  * @tparam T type of referenced silo's data
  */
trait SiloRef[T] {

  def id: SiloRefId

  /** Build graph and send graph to node which contains `this` Silo */
  def send(): Future[T]

  final override def hashCode: Int = id.hashCode

  final override def equals(that: Any): Boolean = that match {
    case other: SiloRef[_] => id == other.id
    case _                 => false
  }

}

abstract class SiloRefAdapter[T]() extends SiloRef[T] with Logging {

  import logger._
  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.pickling._
  import Defaults._
  import binary._
  import graph.Picklers._

  protected def system: silt.Internals

  protected def node: graph.Node

  override def send(): Future[T] = {
    debug(s"Sending graph to host `${id.at}`...")
    system.request(id.at) { msgId => Traverse(msgId, node) } map {
      case Traversed(_, v) => v.asInstanceOf[T]
      case _               => throw new Exception(s"Computation at `${id.at}` failed.")
    }
  }

}

class Materialized[T](refId: RefId, at: Host)(protected val system: silt.Internals) extends SiloRefAdapter[T] {

  override val id = SiloRefId(refId, at)

  override def node(): graph.Node = new graph.Materialized(id)

}

// vim: set tw=120 ft=scala:
