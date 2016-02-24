package fp
package impl
package netty

import java.io.ByteArrayOutputStream

import scala.pickling._
import Defaults._
import binary._

import fp.model.Message

import _root_.io.netty.buffer.ByteBuf
import _root_.io.netty.channel.{ ChannelHandlerContext, ChannelHandler }
import _root_.io.netty.handler.codec.{ ByteToMessageDecoder, MessageToByteEncoder }
import _root_.com.typesafe.scalalogging.{ StrictLogging => Logging }

/** Converts F-P system messages to a format suitable for transmission.
  *
  * Note: Once a message has been encoded, it will be ''automatically'' released
  * by the Netty codec framework. Cf.[[_root_.io.netty.handler.codec.MessageToByteEncoder#write]]
  */
@ChannelHandler.Sharable
private[netty] class Encoder extends MessageToByteEncoder[Message] with Logging {

  import logger._

  /** Called with the outbound message (of type [[fp.model.Message]]) that this
    * class will encode to a [[_root_.io.netty.buffer.ByteBuf]].
    * The [[_root_.io.netty.buffer.ByteBuf]] is then forwarded to the next
    * [[_root_.io.netty.channel.ChannelOutboundHandler]] in the pipeline.
    */
  override def encode(ctx: ChannelHandlerContext, msg: Message, out: ByteBuf): Unit = {
    trace(s"Encoding message: $msg")
    out.writeBytes(pickle(msg))
  }

  def pickle[T <: Message: Pickler](msg: T): Array[Byte] = msg.pickle.value

}

/** Converts a network stream, encoded by [[Encoder]], back to the F-P
  * system message format.
  *
  * Note: [[_root_.io.netty.handler.codec.ByteToMessageDecoder MUST NOT]]
  * be annotated with @Sharable.
  *
  * Note: Once a message has been decoded, it will be "automatically" released
  * by this decoder ([[_root_.io.netty.handler.codec.ByteToMessageDecoder Pitfalls]]).
  */
private[netty] class Decoder extends ByteToMessageDecoder with Logging {

  import java.util.{ List => JList }
  import logger._

  /** Called with a [[ByteBuf]] containing incoming data and a List to which
    * decoded messages are added. This call is repeated until it is determined
    * that no new items have been added to the List or no more bytes are readable
    * in the ByteBuf. Then, if the List is not empty, its contents are passed to
    * the next handler in the pipeline.
    */
  override def decode(ctx: ChannelHandlerContext, in: ByteBuf, out: JList[Object]): Unit = try {

    val buf: ByteBuf = in.readBytes(in.readableBytes())
    val arr: Array[Byte] = if (buf.hasArray()) buf.array() else {
      val bos = new ByteArrayOutputStream
      while (buf.isReadable()) bos.write(buf.readByte())
      bos.toByteArray()
    }

    if (!arr.isEmpty) {
      val msg = BinaryPickle(arr).unpickle[Message]
      trace(s"Decoded message: $msg")
      out add msg
    } else () // nop
    buf.release()
  } catch { case e: Throwable => throw e }

}

