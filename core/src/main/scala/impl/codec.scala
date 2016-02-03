package silt
package impl

import java.io.ByteArrayOutputStream

import scala.pickling._
import scala.pickling.Defaults._
import binary._

import _root_.io.netty.buffer.ByteBuf
import _root_.io.netty.channel.{ ChannelFutureListener, ChannelHandler, ChannelHandlerContext }
import _root_.io.netty.handler.logging.{ LogLevel, LoggingHandler => Logger }
import _root_.io.netty.handler.codec.{ ByteToMessageDecoder, MessageToByteEncoder }

import _root_.com.typesafe.scalalogging.{ StrictLogging => Logging }

/** Encoder converts F-P system messages to a format suitable for transmission.
  *
  * Note: Once a message has been encoded, it will be ''automatically'' released
  * by the Netty codec framework.  Cf.
  * [[io.netty.handler.codec.MessageToByteEncode#write]] 
  */
@ChannelHandler.Sharable
private[impl] class SystemMessageEncoder extends MessageToByteEncoder[silt.Message] with Logging {

  import logger._

  /* Called with the outbound message (of type [[silt.Message]]) that this class
   * will encode to a [[_root_.io.netty.buffer.ByteBuf]]. The
   * [[_root_.io.netty.buffer.ByteBuf]] is then forwarded to the next
   * [[_root_.io.netty.channel.ChannelOutboundHandler]] in the pipeline.
   *
   */
  override def encode(ctx: ChannelHandlerContext, msg: silt.Message, out: ByteBuf): Unit = {
    trace(s"Encoder received message: $msg")
    out.writeBytes(pickle(msg))
  }

  def pickle[T <: silt.Message: Pickler](msg: T): Array[Byte] =
    msg.pickle.value

  // XXX Why not just `msg.pickle.value`?
  //def pickle[T <: silt.Message: Pickler](msg: T): Array[Byte] = {
  //  val pickler = implicitly[Pickler[T]]
  //  val tag = pickler.tag
  //  val builder = pickleFormat.createBuilder()

  //  trace(s"Pickler tag: ${tag.key}")
  //  trace(s"Pickler class: ${pickler.getClass.getName}")

  //  builder.hintTag(tag)
  //  pickler.pickle(msg, builder)

  //  val picklee = builder.result().value
  //  trace(s"Picklee:\n$picklee")

  //  picklee//.getBytes
  //}

}

/** Decoder converts a network stream, encoded by [[SystemMessageEncoder]] back
  * to the F-P system message format.
  *
  * Note: [[io.netty.handler.codec.ByteToMessageDecoder MUST NOT]] be annotated
  * with @Sharable.
  *
  * Note: Once a message has been decoded, it will be ''automatically'' released
  * by this decoder ([[io.netty.handler.codec.ByteToMessageDecoder Pitfalls]]).
  */
private[impl] class SystemMessageDecoder extends ByteToMessageDecoder with Logging {

  import java.util.{ List => JList }
  import logger._

  /* Called with a [[ByteBuf]] containing incoming data and a List to which
   * decoded messages are added. This call is repeated until it is determined
   * that no new items have been added to the List or no more bytes are readable
   * in the ByteBuf. Then, if the List is not empty, its contents are passed to
   * the next handler in the pipeline.
   */
  override def decode(ctx: ChannelHandlerContext, in: ByteBuf, out: JList[Object]): Unit = 
    try {
      val buf: ByteBuf = in.readBytes(in.readableBytes())
      val arr: Array[Byte] = if (buf.hasArray()) buf.array() else {
        val bos = new ByteArrayOutputStream
        while (buf.isReadable()) bos.write(buf.readByte())
        bos.toByteArray()
      }

      if (!arr.isEmpty) {
        val msg = BinaryPickle(arr).unpickle[silt.Message]
        trace(s"Decoder received message: $msg")
        out add msg
      } else () // nop

      buf.release()
    } catch { case e: Throwable => throw e }

}

// vim: set tw=80 ft=scala:
