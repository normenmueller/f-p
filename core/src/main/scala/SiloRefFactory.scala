package fp

import fp.model.Populate

import scala.concurrent.Future
import scala.pickling.Pickler

/** Interface implemented by [[fp.SiloSystem]], the only two places
  * from which you can get new silos.
  */
trait SiloRefFactory {

  /** Upload a silo to `host` with the initialization process defined by `fun`.
    *
    * @tparam T Type of data to be populated
    * @param fun Silo initialization logic
    * @param at Target host of the the to be created silo
    * @return [[fp.SiloRef]], which identifies the created silo
    */
  def populate[T](at: Host)(fun: () => Silo[T])(implicit pickler: Pickler[Populate[T]]): Future[SiloRef[T]]

  /** Uploads a silo to a `Host` populated by `factory`.
    *
    * @param factory Silo initialization logic
    * @param at Target host where the `Silo` will be created
    * @return [[fp.SiloRef]], which identifies the created silo
    */
  final def populate[T](at: Host, factory: SiloFactory[T])(implicit pickler: Pickler[Populate[T]]): Future[SiloRef[T]] =
    populate(at) { () => new Silo[T](factory.data) }
}
