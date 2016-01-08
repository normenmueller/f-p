package silt

import java.rmi.dgc.VMID

import scala.concurrent.Future
import scala.spores.Spore
import scala.pickling.{ Pickler, Unpickler }

trait SiloRefFactory {

  // XXX

}

/** Immutable and serializable handle to a silo.
  *
  * The referenced silo may or may not reside on the local host or inside the
  * same silo system. A [[SiloRef]] can be obtained from [[SiloRefFactory]], an
  * interface which is implemented by [[SiloSystem]].
  */
trait SiloRef[T] {

  def at: Host

  val id: Id = Id(ids.incrementAndGet(), at, JID)

  //XXX def apply[S](fun: Spore[T, S])(implicit pickler: Pickler[Spore[T, S]], unpickler: Unpickler[Spore[T, S]]): SiloRef[S]

  //XXX def flatMap[S](fun: Spore[T, SiloRef[S]])(implicit pickler: Pickler[Spore[T, SiloRef[S]]], unpickler: Unpickler[Spore[T, SiloRef[S]]]): SiloRef[S]

  //XXX def send(): Future[T]

  //XXX def pumpTo[V, R <: Traversable[V], P <: Spore2[W, Emitter[V], Unit]](destSilo: SiloRef[V, R])(fun: P)(implicit bf: BuilderFactory[V, R], pickler: Pickler[P], unpickler: Unpickler[P]): Unit = ???

  /* A JVM-wide, unique identifier of a silo.
   *
   * Note, the context of a host and a VMID is necessary to unambiguously
   * identify silos without the requirement to request consensus in a silo
   * landscape spread over various nodes which, for sure, would negatively
   * affect performance.
   *
   * Note: Not `final` due to https://issues.scala-lang.org/browse/SI-4440
   */
  sealed case class Id(uid: Int, at: Host, in: VMID) {

    override val toString = s"$uid:$in @ $at"

  }

  final override def hashCode: Int = id.hashCode

  final override def equals(that: Any): Boolean = that match {
    case other: SiloRef[_] => id == other.id
    case _                 => false
  }

}

// vim: set tw=80 ft=scala:
