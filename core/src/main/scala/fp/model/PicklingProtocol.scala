package fp
package model

import fp.core.LineagePickling

import scala.pickling._
import scala.pickling.internal.HybridRuntime
import scala.pickling.json.{JsonFormats}
import scala.spores._
import scala.pickling.binary.BinaryFormats
import scala.pickling.pickler.AllPicklers

/** Gather all the logic from [[scala.pickling]] to work for f-p.
  *
  * Ensures that all the imports are in place and that the library doesn't
  * fall back to dynamic generation of picklers.
  */
object PicklingProtocol extends {
  val ignoreMe = internal.replaceRuntime(new HybridRuntime)
} with Ops with AllPicklers with JsonFormats {

  /** Very important, since it solves an optimization issue */
  implicit val so = static.StaticOnly

  /* Direct access to the picklers of spores, import to use */
  val sporesPicklers = SporePickler
  val nodesPicklers = LineagePickling

}

