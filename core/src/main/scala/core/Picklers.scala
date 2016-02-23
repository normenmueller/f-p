package silt
package core

import scala.language.existentials
import scala.pickling.Defaults._
import scala.pickling._
import scala.reflect.runtime

object PickleAlias {
  type PickleFunction[T] = (T, PBuilder) => Unit
}

import silt.core.PickleAlias._

trait Tagged[T] {
  def tag: FastTypeTag[T]
}

trait Helpers {
  /** Used when we need to pickle a type whose type parameters
    * will be erased by the JVM because of the type erasure. */
  def rawFastTypeTag[T](v: T): FastTypeTag[Any] =
    FastTypeTag.mkRaw(v.getClass, runtime.currentMirror)
      .asInstanceOf[FastTypeTag[Any]]
}

trait Writer[T] extends Pickler[T] with Tagged[T] with Helpers {
  def elemsToPickle: Seq[(String, PickleFunction[T])]

  def pickle(picklee: T, builder: PBuilder): Unit = {
    builder.beginEntry(picklee)
    elemsToPickle.foreach {
      (t: (String, PickleFunction[T])) => {
        val (fieldName, f) = t
        builder.putField(fieldName, f(picklee, _))
      }
    }
    builder.endEntry()
  }

}

object Picklers {
  implicit object LineageWriter extends Writer[Lineage] {

    def tag: FastTypeTag[Lineage] = implicitly[FastTypeTag[Lineage]]

    def pickleNode(picklee: Lineage, builder: PBuilder) = {
      val node = picklee.node
      val tag = node match {
        case m: Materialized =>
          implicitly[FastTypeTag[Materialized]]

        /*case a: Apply[U, T, V, S] =>
          rawFastTypeTag[Apply[U, T, V, S], Any](a)

        case mi: MultiInput[U, T, V, S] =>
          rawFastTypeTag[Apply[U, T, V, S], Any](mi)*/
      }
      builder.hintTag(tag)
      implicitly[Pickler[Node]].pickle(node, builder)
    }

    final val elemsToPickle = List("node" -> pickleNode _)

  }

  /** Overrides [[Writer]] so that it uses the specialized reader for
    * the concrete [[Node]] that we want to pickle/unpickle.
    */
  implicit object NodeWriter extends Writer[Node] {

    def tag: FastTypeTag[Node] = implicitly[FastTypeTag[Node]]

    final val elemsToPickle = Seq()

    override def pickle(picklee: Node, builder: PBuilder): Unit =
      picklee match {
        case m: Materialized =>
          implicitly[Pickler[Materialized]].pickle(m, builder)
      }
  }

}
