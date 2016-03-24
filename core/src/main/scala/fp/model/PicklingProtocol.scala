package fp
package model

import scala.pickling._
import scala.spores._
import scala.pickling.binary.BinaryFormats
import scala.pickling.pickler.{AllPicklers, RefPicklers}

/** Gather all the logic from [[scala.pickling]] to work for f-p.
  *
  * Ensures that all the imports are in place and that the library doesn't
  * fall back to dynamic generation of picklers.
  */
object PicklingProtocol extends Ops
  with AllPicklers with RefPicklers with BinaryFormats {

  /** Very important, since it solves an optimization issue */
  /* implicit val so = static.StaticOnly */

  /* Direct access to the picklers of spores, import to use */
  val sporesPicklers = SporePickler

}

