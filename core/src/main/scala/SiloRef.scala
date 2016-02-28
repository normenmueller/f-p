package fp

import com.typesafe.scalalogging.{ StrictLogging => Logging }
import fp.core._
import fp.model.{ Traverse, Traversed }

import scala.concurrent.{ ExecutionContext, Future }

/** A `Silo` is uniquely identified by an `id`. */
private[fp] case class SiloRefId(id: RefId, at: Host)

/** Immutable and serializable handle to a silo.
  *
  * The referenced silo may or may not reside on the local host or inside
  * the same silo system. A [[SiloRef]] can be obtained from [[SiloRefFactory]],
  * an interface which is implemented by [[SiloSystem]].
  *
  * @tparam T Type of data populated in the `Silo`
  */
trait SiloRef[T] {

  def id: SiloRefId

  /** Build graph and send graph to node which contains `this` Silo */
  def send(): Future[T]

  final override def hashCode: Int = id.hashCode

  final override def equals(that: Any): Boolean = that match {
    case other: SiloRef[_] => id == other.id
    case _ => false
  }

}

abstract class SiloRefAdapter[T] extends SiloRef[T] with Logging {

  import logger._
  import fp.core._
  import scala.pickling.Defaults._
  import scala.concurrent.ExecutionContext.Implicits.global

  protected def system: fp.Internals

  protected def node: Node

  override def send(): Future[T] = {
    debug(s"Sending graph to host `${id.at}`...")
    system.request(id.at) { msgId => Traverse(msgId, node) } map {
      case Traversed(_, v) => v.asInstanceOf[T]
      case _ => throw new Exception(s"Computation at `${id.at}` failed.")
    }
  }

}

class MaterializedSilo[T](refId: RefId, at: Host)(protected val system: fp.Internals) extends SiloRefAdapter[T] {

  override val id = SiloRefId(refId, at)

  override def node = Materialized(refId)

}

