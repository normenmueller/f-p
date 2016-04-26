package fp.model.pickling

import fp.{Silo, SiloRefId}
import fp.core._
import fp.model._

import scala.pickling._
import scala.pickling.internal.HybridRuntime
import scala.pickling.json.JsonFormats
import scala.pickling.pickler.AllPicklers
import scala.pickling.AutoRegister
import scala.spores._
import fp.debug

abstract class PicklingLogic extends Ops with AllPicklers with JsonFormats

/** Gather all the logic from [[scala.pickling]] to work for f-p.
  *
  * Ensures that all the imports are in place and that the library doesn't
  * fall back to dynamic generation of picklers.
  */
object PicklingProtocol extends {
  //val currentRuntime = new HybridRuntime
  //val onlyLookup = internal.replaceRuntime(currentRuntime)
} with PicklingLogic {

  /** Very important, since it solves an optimization issue */
  implicit val so = static.StaticOnly

  /* Direct access to the picklers of spores, import to use */
  val sporesPicklers = SporePickler
  import fp.util.PicklingHelper._
  import sporesPicklers._

  implicit val msgIdPickler = implicitly[Pickler[MsgId]]
  implicit val msgIdUnpickler = implicitly[Unpickler[MsgId]]

  implicit val siloSystemIdPickler = implicitly[Pickler[SiloSystemId]]
  implicit val siloSystemIdUnpickler = implicitly[Unpickler[SiloSystemId]]

  implicit def transformedPicklerUnpickler[T: FastTypeTag]
    (implicit p: Pickler[T], u: Unpickler[T])
      : AbstractPicklerUnpickler[Transformed[T]] = {
    implicit object TransformedPU extends AbstractPicklerUnpickler[Transformed[T]]
      with AutoRegister[Transformed[T]] {

      val picklerT = p
      val unpicklerT = u
      override def tag = implicitly[FastTypeTag[Transformed[T]]]

      override def pickle(picklee: Transformed[T], builder: PBuilder): Unit = {

        builder.beginEntry(picklee, tag)
        writeEliding(builder, "id", picklee.id, msgIdPickler)
        writeEliding(builder, "senderId", picklee.senderId, siloSystemIdPickler)
        writeEliding(builder, "data", picklee.data, picklerT)
        builder.endEntry()

      }

      override def unpickle(tag: String, reader: PReader): Any = {
        val id = readEliding(reader, "id", msgIdUnpickler)
        val senderId = readEliding(reader, "senderId", siloSystemIdUnpickler)
        val data = readEliding(reader, "data", unpicklerT)
        Transformed(id, senderId, data)
      }
    }
    TransformedPU
  }

  val refIdPickler = implicitly[Pickler[SiloRefId]]
  val refIdUnpickler = implicitly[Unpickler[SiloRefId]]

  implicit object NodePicklerUnpickler extends AbstractPicklerUnpickler[Node] {

    override def tag = implicitly[FastTypeTag[Node]]

    override def pickle(picklee: Node, builder: PBuilder): Unit = {

      picklee match {
        case mt: Materialized =>
          MaterializedPicklerUnpickler.pickle(mt, builder)

        /* Map and FlatMap need to get the [[FastTypeTag]]
         * the nodes, created when the types were known for
         * the compiler. Otherwise new tags will be created
         * using the type parameters `t` and `s`, leading to
         * some bugs like issue #417 in scala-pickling. */

        case mp: Map[t, s] =>
          implicit val ft: FastTypeTag[t] = mp.ftt
          implicit val fs: FastTypeTag[s] = mp.fts
          mapPicklerUnpickler[t, s].pickle(mp, builder)

        case fp: FlatMap[t, s] =>
          implicit val ft: FastTypeTag[t] = fp.ftt
          implicit val fs: FastTypeTag[s] = fp.fts
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

  def transformationPickle[T, S, N <: Transformation[T, S]]
    (picklee: N, tag: FastTypeTag[N], spPickler: Pickler[Spore[T, S]], builder: PBuilder) = {

    builder.beginEntry(picklee, tag)

    writeEliding(builder, "nodeId", picklee.nodeId.value, stringPickler)
    /* Don't elide types here, since are needed when unpickling */
    write[Node](builder, "target", picklee.target, NodePicklerUnpickler)
    write(builder, "f", picklee.f, spPickler)

    builder.endEntry()

  }

  def transformationUnpickle[T, S]
    (reader: PReader, spUnpickler: Unpickler[Spore[T, S]]): (NodeId, Node, Spore[T, S]) = {
    (NodeId(readEliding(reader, "nodeId", stringPickler)),
      read(reader, "target", NodePicklerUnpickler),
      read(reader, "f", spUnpickler))
  }

  implicit def mapPicklerUnpickler[T: FastTypeTag, S: FastTypeTag]
    (implicit p: Pickler[Spore[T, S]], u: Unpickler[Spore[T, S]])
      : Pickler[Map[T, S]] with Unpickler[Map[T, S]] = {
    implicit object MapPU extends AbstractPicklerUnpickler[Map[T, S]]
        with AutoRegister[Map[T, S]] {

      override def tag = implicitly[FastTypeTag[Map[T, S]]]
      println(implicitly[FastTypeTag[T]].key)

      override def pickle(picklee: Map[T, S], builder: PBuilder): Unit = {
        transformationPickle[T, S, Map[T, S]](picklee, tag, p, builder)
      }

      override def unpickle(tag: String, reader: PReader): Any = {
        val sporeUnpickler = implicitly[Unpickler[Spore[T, S]]]
        val (id, node, sp) = transformationUnpickle(reader, sporeUnpickler)
        Map(node, sp, id)
      }
    }
    MapPU
  }

  implicit def flatMapPicklerUnpickler[T: FastTypeTag, S: FastTypeTag]
    (implicit p: Pickler[Spore[T, Silo[S]]], u: Unpickler[Spore[T, Silo[S]]])
      : Pickler[FlatMap[T, S]] with Unpickler[FlatMap[T, S]] = {
    implicit object FlatMapPU extends AbstractPicklerUnpickler[FlatMap[T, S]]
        with AutoRegister[FlatMap[T, S]] {

      override def tag = implicitly[FastTypeTag[FlatMap[T, S]]]

      override def pickle(picklee: FlatMap[T, S], builder: PBuilder): Unit = {
        transformationPickle[T, Silo[S], FlatMap[T, S]](picklee, tag, p, builder)
      }

      override def unpickle(tag: String, reader: PReader): Any = {
        val sporeUnpickler = implicitly[Unpickler[Spore[T, Silo[S]]]]
        val (id, node, sp) = transformationUnpickle(reader, sporeUnpickler)
        FlatMap(node, sp, id)
      }
    }
    FlatMapPU
  }

}

