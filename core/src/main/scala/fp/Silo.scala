package fp

import fp.SiloFactory.SiloGen
import fp.backend.SiloSystem
import fp.model.{PicklingProtocol, Populate, Populated}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions
import scala.spores._

/** Container of some data stored in a node.
  *
  * [[Silo]]s only exist on the server side, that is, the node that
  * is storing data. A program that is being executed on any node
  * that wants to access this data can only do so using a reference to
  * that silo, the so-called [[SiloRef]].
  *
  * [[Silo]]s are not public and cannot be created directly.
  */
/* Disable temporarily since this part of the API is not finished
 * because we have problems with pickling/unpickling `SporeWithEnv`s
 * private[fp] class Silo[T](private[fp] val data: T)
 * */ 
final case class Silo[T](val data: T) {
  type D = T
  def map[U](f: T => U): Silo[U] = flatMap(d => new Silo(f(d)))
  def flatMap[U](f: T => Silo[U]): Silo[U] = f(data)
  def getSpecialize: Silo[T] = this.asInstanceOf[Silo[T]]
}

import scala.pickling._
/** [[Silo]]s can only be created through a [[SiloFactory]],
  * which are used to generate [[Silo]]s on a concrete host node.
  */
/* As a workaround of a bug in scala-pickling, make constructor public
 * class SiloFactory[T: Pickler] private[fp](val s: SiloGen[T]) {
 * */
class SiloFactory[T: Pickler: Unpickler](val s: SiloGen[T]) {

  import PicklingProtocol._
  import sporesPicklers._

  val pt = implicitly[Pickler[Silo[T]]]
  val ut = implicitly[Unpickler[Silo[T]]]

  def populateAt(at: Host)(implicit system: SiloSystem,
                           ec: ExecutionContext): Future[SiloRef[T]] = {
    system.request(at) { Populate(_, s) } map {
      case Populated(_, node) => new MaterializedSilo[T](node, at)
      case _ => throw new Exception(s"Silo population at `$at` failed.")
    }
  }

}

/** The only source of creation of [[Silo]]s. */
object SiloFactory extends SiloFactoryHelpers {

  type SiloGen[T] = Spore[Unit, Silo[T]]

  /** Create a [[SiloFactory]] explicitly from a function [[Unit => T]].
    *
    * NOTE: It's good to make this explicit to constrain the amount of magic
    * we do with implicits, so that if the users prefer this arguably more
    * readable notation, they can pick it.
    *
    * NOTE: There isn't a constructor for the by-value parameter because of two
    * reasons: clearer to have a direct conversion [[T]] => [[SiloFactory]]
    * and there's a type conflict (same function type after erasure).
    * Possible solution: magnet pattern.
    */
  def apply[T: Pickler: Unpickler](dataGen: () => T): SiloFactory[T] =
    fromFunctionToSiloFactory(dataGen)
}

trait SiloFactoryHelpers {

  private final def siloGenerator[T: Pickler: Unpickler](f: () => T): SiloGen[T] = {
    spore[Unit, Silo[T]] {
      val gen = f
      Unit => new Silo[T](gen())
    }
  }

  /** Converts a function that returns some data to a [[SiloFactory]].
    *
    * This is useful to avoid the user to explicitly create a new instance of
    * [[SiloFactory]] to wrap such a function. Syntactic sugar for the API.
    */
  implicit def fromFunctionToSiloFactory[T: Pickler: Unpickler](f: () => T): SiloFactory[T] = {
    new SiloFactory(siloGenerator(f))
  }

  /** Directly converts some data to a [[SiloFactory]].
    *
    * This is useful to avoid the user to explicitly create a new instance of
    * [[SiloFactory]] to wrap such a collection. Syntactic sugar for the API.
    */
  implicit def fromByValueToSiloFactory[T: Pickler: Unpickler](p: => T): SiloFactory[T] = {
    new SiloFactory(siloGenerator(() => p))
  }

}

