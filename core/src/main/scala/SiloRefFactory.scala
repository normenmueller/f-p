package silt

import scala.concurrent.Future
import scala.pickling.{ Pickler, Unpickler }

/** Interface implemented by [[SiloSystem]], the only two places from which you can get fresh silos. */
trait SiloRefFactory {

  /** Upload a silo to `host` with the initialization process defined by `fun`.
    *
    * @tparam T
    *
    * @param fun silo initialization logic
    * @param at target host of the the to be created silo
    *
    * @return a [[silt.SiloRef]], identifying the uploaded silo, as soon as the initialization process has been
    * completed.
    */
  def populate[T](at: Host)(fun: () => Silo[T])(implicit pickler: Pickler[Populate[T]]): Future[SiloRef[T]]

  /** Uploads a silo to `host` with the initialization process of `fac`.
    *
    * @param fac silo initialization logic
    * @param at target host of the the to be created silo
    *
    * @return a [[silt.SiloRef]], identifying the uploaded silo, as soon as the initialization process has been
    * completed.
    */
  final def populate[T](at: Host, fac: SiloFactory[T])(implicit pickler: Pickler[Populate[T]]): Future[SiloRef[T]] =
    populate(at) { () => new Silo[T](fac.data) }

}

// vim: set tw=120 ft=scala:
