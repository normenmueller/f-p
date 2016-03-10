package fp
package model

import scala.pickling._
import scala.spores._
import scala.pickling.binary.BinaryFormats
import scala.pickling.pickler.{AllPicklers, RefPicklers}

/** Gather all the logic from [[scala.pickling]] to work for f-p.
  *
  * Ensures that all the imports are in place and that the library doesn't
  * fall back to dynamic generation of picklers. Import `picklingProtocol`.
  */
object SimplePicklingProtocol extends Ops with AllPicklers with RefPicklers with BinaryFormats {
  /** Very important, since it solves an optimization issue */
  implicit val so = static.StaticOnly
}

object PicklingProtocol extends Ops with AllPicklers with RefPicklers with BinaryFormats
    with SporePickler with SimpleSporePicklerImpl {

  /** Very important, since it solves an optimization issue */
  implicit val so = static.StaticOnly

  ////////////////////////////////////////////////////////////////////////////
  /////////////////// Chunk copied from scala spores-pickling ////////////////
  ////////////////////////////////////////////////////////////////////////////

  def genSporePickler[T, R, U](implicit cPickler: Pickler[U], cUnpickler: Unpickler[U])
  : Pickler[Spore[T, R] { type Captured = U }] with Unpickler[Spore[T, R] { type Captured = U }] = macro genSporePicklerImpl[T, R, U]

  implicit def genSimpleSporePickler[T, R]: Pickler[Spore[T, R]] =
  macro genSimpleSporePicklerImpl[T, R]

  implicit def genSimpleSpore2Pickler[T1, T2, R]: Pickler[Spore2[T1, T2, R]] =
  macro genSimpleSpore2PicklerImpl[T1, T2, R]

  implicit def genSimpleSpore3Pickler[T1, T2, T3, R]: Pickler[Spore3[T1, T2, T3, R]] =
  macro genSimpleSpore3PicklerImpl[T1, T2, T3, R]

  // type `U` </: `Product`
  implicit def genSporeCSPickler[T, R, U](implicit cPickler: Pickler[U]): Pickler[SporeWithEnv[T, R] { type Captured = U }] =
  macro genSporeCSPicklerImpl[T, R, U]

  // TODO: probably need also implicit macro for Pickler[Spore[T, R] { type Captured = U }]
  // capture > 1 variables
  implicit def genSporeCMPickler[T, R, U <: Product](implicit cPickler: Pickler[U], cUnpickler: Unpickler[U])
  : Pickler[SporeWithEnv[T, R] { type Captured = U }] with Unpickler[SporeWithEnv[T, R] { type Captured = U }] = macro genSporeCMPicklerImpl[T, R, U]

  // capture > 1 variables
  implicit def genSpore2CMPickler[T1, T2, R, U <: Product](implicit cPickler: Pickler[U], cUnpickler: Unpickler[U])
  : Pickler[Spore2WithEnv[T1, T2, R] { type Captured = U }] = macro genSpore2CMPicklerImpl[T1, T2, R, U]

  // capture > 1 variables
  implicit def genSpore3CMPickler[T1, T2, T3, R, U <: Product](implicit cPickler: Pickler[U], cUnpickler: Unpickler[U])
  : Pickler[Spore3WithEnv[T1, T2, T3, R] { type Captured = U }] = macro genSpore3CMPicklerImpl[T1, T2, T3, R, U]

  implicit def genSporeCSUnpickler[T, R]: Unpickler[Spore[T, R]/* { type Captured }*/] =
  macro genSporeCSUnpicklerImpl[T, R]

  implicit def genSpore2CSUnpickler[T1, T2, R]: Unpickler[Spore2[T1, T2, R]] =
  macro genSpore2CSUnpicklerImpl[T1, T2, R]

  implicit def genSpore2CMUnpickler[T1, T2, R, U]: Unpickler[Spore2WithEnv[T1, T2, R] { type Captured = U }] =
  macro genSpore2CMUnpicklerImpl[T1, T2, R, U]

  implicit def genSporeCMUnpickler[T, R, U]: Unpickler[SporeWithEnv[T, R] { type Captured = U }] =
  macro genSporeCMUnpicklerImpl[T, R, U]

  implicit def genSpore3CSUnpickler[T1, T2, T3, R]: Unpickler[Spore3[T1, T2, T3, R]] =
  macro genSpore3CSUnpicklerImpl[T1, T2, T3, R]

}

