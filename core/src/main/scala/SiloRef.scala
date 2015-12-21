package silt

import scala.concurrent.Future
import scala.spores.Spore
import scala.pickling.{ Pickler, Unpickler }

/** Immutable and serializable handle to a silo. 
 *
 *  The referenced silo may or may not reside on the local host or inside the
 *  same silo system. A [[SiloRef]] can be obtained from [[SiloRefFactory]], an
 *  interface which is implemented by [[SiloSystem]].
  *
  * @tparam T element type of the referenced silo
  */
trait SiloRef[T] {

  def id: SiloRefId

  @deprecated("use `id.at` instead", "")
  def host: Host =
    id.at

  def apply[S](fun: Spore[T, S])(implicit pickler: Pickler[Spore[T, S]], unpickler: Unpickler[Spore[T, S]]): SiloRef[S]

  def flatMap[S](fun: Spore[T, SiloRef[S]])(implicit pickler: Pickler[Spore[T, SiloRef[S]]], unpickler: Unpickler[Spore[T, SiloRef[S]]]): SiloRef[S]

  def send(): Future[T]

}

/** A JVM-wide, unique identifier of a silo.
  *
  * Note, the context of a host is necessary to unambiguously identify silos
  * without the requirement to request consensus in a silo landscape spread over
  * various nodes which, for sure, would negatively affect performance.
  *
  * XXX SiloRefId uniqueness: Make a SiloRefId JVM-wide unique (cf. java.rmi.dgc.VMID)
  */
final case class SiloRefId(at: Host, value: Int) {

  override def toString = s"$at:$value"

}

// vim: set tw=80 ft=scala:
