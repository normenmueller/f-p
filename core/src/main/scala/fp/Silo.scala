package fp

import fp.backend.SiloSystem
import fp.model.{SimplePicklingProtocol, PicklingProtocol, Populate, Populated}

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
private[fp] class Silo[T](private[fp] val data: T)

/** [[Silo]]s can only be created through a [[SiloFactory]],
  * which are used to generate [[Silo]]s on a concrete host node.
  */
class SiloFactory[T] private[fp] (val s: Spore[Unit, Silo[T]]) {

  import SimplePicklingProtocol._
  import SporePickler._

  def populateAt(at: Host)(implicit system: SiloSystem,
                           ec: ExecutionContext): Future[SiloRef[T]] = {
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
    * reasons: clearer to have a direct conversion [[T]] => [[SiloFactory]]
    * and there's a type conflict (same function type after erasure).
    * Possible solution: magnet pattern.
    */
  def apply[T](dataGen: () => T): SiloFactory[T] =
      fromFunctionToSiloFactory(dataGen)

}

trait SiloFactoryHelpers {

  private final def siloGenerator[T](f: () => T): Spore[Unit, Silo[T]] = {
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
  implicit def fromFunctionToSiloFactory[T](f: () => T): SiloFactory[T] =
    new SiloFactory(siloGenerator(f))

  /** Directly converts some data to a [[SiloFactory]].
    *
    * This is useful to avoid the user to explicitly create a new instance of
    * [[SiloFactory]] to wrap such a collection. Syntactic sugar for the API.
    */
  implicit def fromByValueToSiloFactory[T](p: => T): SiloFactory[T] =
      new SiloFactory(siloGenerator(() => p))

}

