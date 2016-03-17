package fp
package util

import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/** Generator that produces values of type `T` */
trait Gen[T] {
  def next: T
}

/** Generates [[Int]]s sequentially starting from zero in a thread-safe way.
  * Create an independent object every time you want to use it, since you
  * don't probably want to share the generator.
  */
trait IntGen extends Gen[Int] {
  private lazy val ids = new AtomicInteger(0)
  def next: Int = ids.incrementAndGet()
}

/** Generates [[UUID]]s, which are in turn random string values */
trait UUIDGen extends Gen[String] {
  def next = UUID.randomUUID().toString
}

object UUIDGen extends UUIDGen
