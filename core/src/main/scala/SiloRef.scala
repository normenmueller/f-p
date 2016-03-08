package fp

import com.typesafe.scalalogging.{ StrictLogging => Logging }
import fp.core._
import fp.model.{ PicklingProtocol, Traverse, Traversed }

import scala.concurrent.Future

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

  /** Build graph and send it to the node that stores the referenced [[Silo]] */
  def send: Future[T]

  final override def hashCode: Int = id.hashCode

  final override def equals(that: Any): Boolean = that match {
    case other: SiloRef[_] => id == other.id
    case _ => false
  }

}

abstract class SiloRefAdapter[T] extends SiloRef[T] with PicklingProtocol with Logging {

  import fp.core._
  import logger._

  import picklingProtocol._
  import scala.concurrent.ExecutionContext.Implicits.global

  protected def backend: BackendLogic
  protected def node: Node

  override def send: Future[T] = {
    debug(s"Sending graph to host `${id.at}`...")
    backend.request(id.at) { msgId => Traverse(msgId, node) } map {
      case Traversed(_, v) => v.asInstanceOf[T]
      case _ => throw new Exception(s"Computation at `${id.at}` failed.")
    }
  }

}

class MaterializedSilo[T](refId: RefId, at: Host)(protected val backend: BackendLogic)
    extends SiloRefAdapter[T] {

  override val id = SiloRefId(refId, at)
  override def node = Materialize(refId)

}

