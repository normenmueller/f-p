package fp

import scala.concurrent.Future
import scala.pickling.{Unpickler, Pickler}
import scala.spores.Spore

import fp.core._
import fp.util.UUIDGen
import fp.backend.SiloSystem
import fp.model.{PicklingProtocol, Transform, Transformed}

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
  def send(implicit p: Pickler[Silo[T]], u: Unpickler[Silo[T]]): Future[T]

  def map[S](f: Spore[T, S])
    (implicit ps: Pickler[Spore[T,S]], us: Unpickler[Spore[T, S]]): SiloRef[S]

  def flatMap[S](f: Spore[T, SiloRef[S]])
    (implicit ps: Pickler[Spore[T,SiloRef[S]]], us: Unpickler[Spore[T, SiloRef[S]]]): SiloRef[S]

  final override def hashCode: Int = id.hashCode

  final override def equals(that: Any): Boolean = that match {
    case other: SiloRef[_] => id == other.id
    case _ => false
  }

}

abstract class SiloRefAdapter[T] extends SiloRef[T] with Logging {

  import logger._
  import PicklingProtocol._
  import sporesPicklers._
  import scala.concurrent.ExecutionContext.Implicits.global

  protected def node: Node
  protected def system: SiloSystem

  override def send(implicit p: Pickler[Silo[T]], u: Unpickler[Silo[T]]): Future[T] = {
    debug(s"Sending graph to host `${id.at}`...")
    implicitly[Pickler[Transform]]
    implicitly[Unpickler[Transform]]
    system.request(id.at) { Transform(_, node) } map {
      case t: Transformed[T] => t.data
      case _ => throw new Exception(s"Computation at `${id.at}` failed.")
    }
  }

  override def map[U](f: Spore[T, U])(implicit ps: Pickler[Spore[T,U]], us: Unpickler[Spore[T, U]]): SiloRef[U] = {
    debug(s"Creating map node targeting $node")
    val mapped = Map(node, f)
    new TransformedSilo(mapped)(system)
  }

  override def flatMap[U](f: Spore[T, SiloRef[U]])(implicit ps: Pickler[Spore[T,SiloRef[U]]], us: Unpickler[Spore[T, SiloRef[U]]]): SiloRef[U] = {
    debug(s"Creating flatMap node targeting $node")
    implicitly[Pickler[SiloRef[U]]]
    implicitly[Unpickler[SiloRef[U]]]
    val flatMapped = FlatMap(node, f)
    new TransformedSilo(flatMapped)(system)
  }

}

class MaterializedSilo[T](override val node: Materialized, at: Host)
                         (implicit val system: SiloSystem) extends SiloRefAdapter[T] {

  override val id = node.refId

}

class TransformedSilo[T](override val node: Node)
                        (implicit val system: SiloSystem) extends SiloRefAdapter[T] {

  @scala.annotation.tailrec
  private def findClosestMaterialized(n: Node): Materialized = {
    n match {
      case m: Materialized => m
      case t: Transformation => findClosestMaterialized(t.target)
    }
  }

  override lazy val id = findClosestMaterialized(node).refId

}

