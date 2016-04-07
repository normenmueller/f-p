package fp
package util

import scala.pickling.Unpickler

object RuntimeHelper {

  /* Instantiate a class for an already generated `Unpickler` */
  def getInstance[T](className: String): T = {
    try {
      val clazz = Class.forName(className)
      println(s"Create instance of $clazz")
      clazz.newInstance().asInstanceOf[T]
    } catch {
      case ex: Throwable =>
        scala.concurrent.util.Unsafe.instance.allocateInstance(
          Class.forName(className)
        ).asInstanceOf[T]
    }
  }

}
