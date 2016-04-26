package multijvm
package cs

import fp.{Host, SiloRefId}
import fp.core.Materialized
import fp.model.pickling.PicklingProtocol
import scala.spores._
import scala.pickling._

trait FPImplicits {
  def debug(s: String) = println(s)

  import scala.pickling.{Pickler, Unpickler}
  import PicklingProtocol._
  import sporesPicklers._
  import fp._
  import fp.model._

  implicit val pl = implicitly[Pickler[List[String]]]
  implicit val pl2 = implicitly[Unpickler[List[String]]]
  implicit val pli = implicitly[Pickler[List[Int]]]
  implicit val pli2 = implicitly[Unpickler[List[Int]]]
  implicit val pls = implicitly[AbstractPicklerUnpickler[Populate[List[String]]]]
  implicit val pp = implicitly[AbstractPicklerUnpickler[Populated]]

}
