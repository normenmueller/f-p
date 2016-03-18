package fp

import fp.core._
import fp.util.{UUIDGen, Gen}
import fp.backend.SiloSystem
import fp.model.{SimplePicklingProtocol, Transform, Transformed}

import scala.concurrent.Future
import com.typesafe.scalalogging.{StrictLogging => Logging}

/** A `Silo` is uniquely identified by an `id`. */
private[fp] final case class SiloRefId(at: Host, id: String = UUIDGen.next)

/** Immutable and serializable handle to a silo.
  *
  * The referenced silo may or may not reside on the local host or inside
  * the same silo system. A [[SiloRef]] can be obtained from [[]],
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

abstract class SiloRefAdapter[T] extends SiloRef[T] with Logging {

  import fp.core._
  import logger._

  import SimplePicklingProtocol._
  import scala.spores.SporePickler._

  import scala.concurrent.ExecutionContext.Implicits.global

  protected def system: SiloSystem
  protected def node: Node

  override def send: Future[T] = {
    debug(s"Sending graph to host `${id.at}`...")
    system.request(id.at) { msgId => Transform(msgId, node) } map {
      case t: Transformed[T] => t.data
      case _ => throw new Exception(s"Computation at `${id.at}` failed.")
    }
  }

}

class MaterializedSilo[T](override val node: Materialized, at: Host)
                         (implicit val system: SiloSystem) extends SiloRefAdapter[T] {

  override val id = node.refId

}

