package fp
package model
package pickling

import fp.core._

import scala.spores._
import scala.pickling._

object LineagePickling {

  import fp.util.PicklingHelper._
  import fp.model.PicklingProtocol._
  import sporesPicklers._

  val refIdPickler = implicitly[Pickler[SiloRefId]]
  val refIdUnpickler = implicitly[Unpickler[SiloRefId]]

  implicit object NodePicklerUnpickler extends Pickler[Node] with Unpickler[Node] {

    override def tag = implicitly[FastTypeTag[Node]]

    override def pickle(picklee: Node, builder: PBuilder): Unit = {

      picklee match {
        case mt: Materialized =>
          MaterializedPicklerUnpickler.pickle(mt, builder)

        case mp: Map[t, s] =>
          mapPicklerUnpickler[t, s].pickle(mp, builder)

        case fp: FlatMap[t, s] =>
          flatMapPicklerUnpickler[t, s].pickle(fp, builder)
      }

    }

    override def unpickle(tag: String, reader: PReader): Any = {

      if (tag.contains("Materialized")) {
        MaterializedPicklerUnpickler.unpickle(tag, reader)
      } else if (tag.contains("FlatMap")) {
        flatMapPicklerUnpickler[Any, Any].unpickle(tag, reader)
      } else if (tag.contains("Map")) {
        mapPicklerUnpickler[Any, Any].unpickle(tag, reader)
      } else {
        // Sorry but runtime errors deserve runtime exceptions
        throw new RuntimeException("Received an unexpected tag " + tag)
      }

    }

  }

  implicit object MaterializedPicklerUnpickler
    extends Pickler[Materialized] with Unpickler[Materialized] {

    override def tag = implicitly[FastTypeTag[Materialized]]

    override def pickle(picklee: Materialized, builder: PBuilder): Unit = {

      builder.beginEntry(picklee, tag)
      writeEliding(builder, "nodeId", picklee.nodeId.value, stringPickler)
      writeEliding(builder, "refId", picklee.refId, refIdPickler)
      builder.endEntry()

    }

    override def unpickle(tag: String, reader: PReader): Any = {

      val id = readEliding(reader, "nodeId", stringPickler)
      val siloRefId = readEliding(reader, "refId", refIdUnpickler)
      Materialized(siloRefId, NodeId(id))

    }

  }

  def transformationPickle[T, S, N <: Transformation[T, S]](picklee: N,
    tag: FastTypeTag[N], spPickler: Pickler[Spore[T, S]], builder: PBuilder) = {

    builder.beginEntry(picklee, tag)

    writeEliding(builder, "nodeId", picklee.nodeId.value, stringPickler)
    /* Don't elide types here, since are needed when unpickling */
    write[Node](builder, "target", picklee.target, NodePicklerUnpickler)
    write(builder, "f", picklee.f, spPickler)

    builder.endEntry()

  }

  def transformationUnpickle[T, S](reader: PReader,
      spUnpickler: Unpickler[Spore[T, S]]): (NodeId, Node, Spore[T, S]) = {
    (NodeId(readEliding(reader, "nodeId", stringPickler)),
      read(reader, "target", NodePicklerUnpickler),
      read(reader, "f", spUnpickler))
  }

  implicit def mapPicklerUnpickler[T, S]
    (implicit p: Pickler[Spore[T, S]], u: Unpickler[Spore[T, S]])
      : Pickler[Map[T, S]] with Unpickler[Map[T, S]] = {
    new Pickler[Map[T, S]] with Unpickler[Map[T, S]] {

      override def tag = implicitly[FastTypeTag[Map[T, S]]]

      override def pickle(picklee: Map[T, S], builder: PBuilder): Unit = {
        transformationPickle[T, S, Map[T, S]](picklee, tag, p, builder)
      }

      private val sporeUnpickler = u
      override def unpickle(tag: String, reader: PReader): Any = {
        val (id, node, sp) = transformationUnpickle(reader, sporeUnpickler)
        Map(node, sp, id)
      }
    }
  }

  implicit def flatMapPicklerUnpickler[T, S]
    (implicit p: Pickler[Spore[T, Silo[S]]], u: Unpickler[Spore[T, Silo[S]]])
      : Pickler[FlatMap[T, S]] with Unpickler[FlatMap[T, S]] = {
    new Pickler[FlatMap[T, S]] with Unpickler[FlatMap[T, S]] {

      override def tag = implicitly[FastTypeTag[FlatMap[T, S]]]

      override def pickle(picklee: FlatMap[T, S], builder: PBuilder): Unit = {
        transformationPickle[T, Silo[S], FlatMap[T, S]](picklee, tag, p, builder)
      }

      private val sporeUnpickler = u
      override def unpickle(tag: String, reader: PReader): Any = {
        val (id, node, sp) = transformationUnpickle(reader, sporeUnpickler)
        FlatMap(node, sp, id)
      }
    }
  }

}
