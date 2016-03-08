package fp
package model

/** Gather all the logic from [[scala.pickling]] to work for f-p.
  *
  * Ensures that all the imports are in place and that the library doesn't
  * fall back to dynamic generation of picklers. Import `picklingProtocol`.
  */
trait PicklingProtocol {

  val picklingProtocol = {
    import scala.pickling._
    new Ops with pickler.AllPicklers // This simulates Default._
    with pickler.RefPicklers with binary.BinaryFormats {
      implicit val so = static.StaticOnly
    }
  }

}
