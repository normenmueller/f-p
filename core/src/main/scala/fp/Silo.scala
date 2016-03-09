package fp

import fp.backend.SiloSystem
import fp.model.{PicklingProtocol, Populate, Populated}

import scala.concurrent.Future
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
private[fp] class Silo[S, T <: Traversable[S]](private[fp] val data: T)

/** [[Silo]]s can only be created through a [[SiloFactory]],
  * which are used to generate [[Silo]]s on a concrete host node.
  */
class SiloFactory[S, T <: Traversable[S]] private[fp] (val s: Spore[Unit, Silo[S, T]])
  extends PicklingProtocol {

  import scala.concurrent.ExecutionContext.Implicits.global
  //
  //import picklingProtocol._
  import scala.pickling.Defaults._

  def populateAt(at: Host)(implicit system: SiloSystem): Future[SiloRef[T]] = {
    system.request(at) { msgId => Populate(msgId, s) } map {
      case Populated(_, ref) => new MaterializedSilo[T](ref, at)(system)
      case _ => throw new Exception(s"Silo population at `$at` failed.")
    }
  }

}

object SiloFactory extends SiloFactoryHelpers {

  /** Create a [[SiloFactory]] explicitly from a function [[Unit => T]].
    *
    * NOTE: It's good to make this explicit to constraint the amount of magic
    * we do with implicits, so that if the users prefer this arguably more
    * readable notation, they can pick it.
    *
    * NOTE: There isn't a constructor for the by-value parameter because of two
    * reasons: clearer to have a direct conversion [[Traversable]] => [[SiloFactory]]
    * and there's a type conflict (same type after erasure).
    * Possible solution: magnet pattern.
    */
  def apply[S, T <: Traversable[S]]
  (dataGen: () => T): SiloFactory[S, T] =
      fromFunctionToSiloFactory(dataGen)

}

trait SiloFactoryHelpers {

  private final def siloGenerator[S, T <: Traversable[S]]
    (f: () => T): Spore[Unit, Silo[S, T]] = {
      spore[Unit, Silo[S,T]] {
        val gen = f
        Unit => new Silo[S,T](gen())
      }
    }

  /** Converts a function that returns a collection to a [[SiloFactory]].
    *
    * This is useful to avoid the user to explicitly create a new instance of
    * [[SiloFactory]] to wrap such a function. Syntactic sugar for the API.
    */
  implicit def fromFunctionToSiloFactory[S, T <: Traversable[S]]
    (f: () => T): SiloFactory[S, T] =
      new SiloFactory(siloGenerator(f))

  /** Directly converts a collection to a [[SiloFactory]].
    *
    * This is useful to avoid the user to explicitly create a new instance of
    * [[SiloFactory]] to wrap such a collection. Syntactic sugar for the API.
    */
  implicit def fromByValueToSiloFactory[S, T <: Traversable[S]]
    (p: => T): SiloFactory[S, T] =
      new SiloFactory(siloGenerator(() => p))

}

