package fp

import fp.model.Populate

import scala.concurrent.Future
import scala.pickling.Pickler

/** Interface implemented by [[fp.SiloSystem]], the only two places
  * from which you can get new silos.
  */
trait SiloRefFactory {

  /** Uploads a silo to a `Host` populated by `factory`.
    *
    * @param factory Silo initialization logic
    * @param at Target host where the `Silo` will be created
    * @return [[fp.SiloRef]], which identifies the created silo
    */
  def populate[S, T <: Traversable[S]](at: Host, factory: SiloFactory[S, T])
   (implicit p: Pickler[Populate[S, T]]): Future[SiloRef[T]]
}
