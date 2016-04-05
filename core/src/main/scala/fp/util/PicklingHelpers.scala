package fp.util

import scala.pickling._

object PicklingHelpers {

  def writeTemplate[T](builder: PBuilder, field: String, value: T,
                       pickler: Pickler[T], sideEffect: PBuilder => Unit) = {
    builder.putField(field, { b =>
      sideEffect(b)
      pickler.pickle(value, b)
    })
  }

  def write[T](builder: PBuilder, field: String, value: T, pickler: Pickler[T]) =
    writeTemplate(builder, field, value, pickler, {b => ()})

  def writeEliding[T](builder: PBuilder, field: String, value: T, pickler: Pickler[T]) =
    writeTemplate(builder, field, value, pickler, {b =>
      b.hintElidedType(pickler.tag)
    })

  def readTemplate[T](reader: PReader, field: String,
                      unpickler: Unpickler[T], sideEffect: PReader => Unit): T = {
    val reader1 = reader.readField(field)
    sideEffect(reader1)
    val tag1 = reader1.beginEntry()
    val result = unpickler.unpickle(tag1, reader1).asInstanceOf[T]
    reader1.endEntry()
    result
  }

  def read[T](reader: PReader, field: String, unpickler: Unpickler[T]): T =
    readTemplate(reader, field, unpickler, {r => ()})

  def readEliding[T](reader: PReader, field: String, unpickler: Unpickler[T]): T =
    readTemplate(reader, field, unpickler, {r =>
      r.hintElidedType(unpickler.tag)
    })

}
