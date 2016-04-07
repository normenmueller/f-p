package fp

import scala.concurrent.Future
import scala.pickling.{Unpickler, Pickler}
import scala.spores.Spore

import fp.core._
import fp.util.UUIDGen
import fp.backend.SiloSystem
import fp.model._

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

  /** Build graph and send it to the node
    * that stores the referenced [[Silo]] */
  def send: Future[T]

  def map[S: Pickler: Unpickler](f: Spore[T, S]): SiloRef[S]

  def flatMap[S: Pickler: Unpickler](f: Spore[T, Silo[S]]): SiloRef[S]

  final override def hashCode: Int = id.hashCode

  final override def equals(that: Any): Boolean = that match {
    case other: SiloRef[_] => id == other.id
    case _ => false
  }

}

abstract class SiloRefAdapter[T: Pickler: Unpickler] extends SiloRef[T] with Logging {

  import logger._
  import PicklingProtocol._
  import sporesPicklers._
  import nodesPicklers._
  import scala.spores._

  protected def node: Node
  protected implicit def system: SiloSystem

  override def map[U: Pickler: Unpickler](f: Spore[T, U]): SiloRef[U] = {
    debug(s"Creating map node targeting $node")

    /*val mapped = node match {
      case m: Map[q, T] =>
        m.copy(f = fuseSpore(m.f, f))
      case fm: FlatMap[q, T] =>
        val wrapper: Spore[Silo[T],U] = (s: Silo[T]) => s.map(f)
        FlatMap[q, U](fm.target, fuseSpore(fm.f, wrapper), fm.nodeId)
    }*/

    val mapped = Map(node, f)
    new TransformedSilo(mapped)
  }

  override def flatMap[U: Pickler: Unpickler](f: Spore[T, Silo[U]]): SiloRef[U] = {
    debug(s"Creating flatMap node targeting $node")

    /*val flatMapped = node match {
      case m: Map[_, T] =>
        val df = m.f andThen  f
        m.copy(f = df)
        FlatMap(m.target, m.f andThen f, m.nodeId)
      case fm: FlatMap[_, T] =>
        fm.copy(f = fm.f andThen {(s: Silo[T]) => s.flatMap(f)})
    }*/

    val flatMapped = FlatMap(node, f)
    new TransformedSilo(flatMapped)
  }

}

class MaterializedSilo[R: Pickler: Unpickler]
    (override val node: Materialized, at: Host)
    (implicit val system: SiloSystem) extends SiloRefAdapter[R] {

  import PicklingProtocol._
  import nodesPicklers._

  import scala.concurrent.ExecutionContext.Implicits.global

  override val id = node.refId

  override def send: Future[R] = {
    debug(s"Requesting data of materialized node to host `${id.at}`...")
    system.request(id.at) { RequestData(_, system.systemId, node) } map {
      case t: Transformed[R] => t.data
      case _ => throw new Exception(s"Computation at `${id.at}` failed.")
    }
  }

}

class TransformedSilo[T, S, R: Pickler: Unpickler]
  (override val node: Transformation[T, S])
  (implicit val system: SiloSystem) extends SiloRefAdapter[R] {

  import PicklingProtocol._
  import nodesPicklers._
  import sporesPicklers._

  import scala.concurrent.ExecutionContext.Implicits.global


  override def send: Future[R] = {
    debug(s"Sending graph to host `${id.at}`...")

    val transformedPickler = implicitly[Pickler[Transformed[R]]]
    val transformedUnpickler = implicitly[Unpickler[Transformed[R]]]
    val picklerClassName = transformedPickler.getClass.getName
    val unpicklerClassName = transformedUnpickler.getClass.getName

    system.request(id.at) {
      Transform(_, system.systemId, node, unpicklerClassName, picklerClassName)
    } map {
      case t: Transformed[R] => t.data
      case _ => throw new Exception(s"Computation at `${id.at}` failed.")
    }

  }

  override lazy val id = node.findClosestMaterialized.refId

}

