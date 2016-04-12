package fp
package util

object RuntimeHelper {

  /* Instantiate a class from a class name */
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
