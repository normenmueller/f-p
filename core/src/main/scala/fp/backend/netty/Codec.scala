package fp
package backend
package netty

import java.io.ByteArrayOutputStream

import fp.model.Message
import fp.model.PicklingProtocol._
import sporesPicklers._

import _root_.io.netty.buffer.ByteBuf
import _root_.io.netty.channel.{ ChannelHandlerContext, ChannelHandler }
import _root_.io.netty.handler.codec.{ ByteToMessageDecoder, MessageToByteEncoder }
import _root_.com.typesafe.scalalogging.{ StrictLogging => Logging }

import scala.pickling.json.JSONPickle

/** Converts F-P system messages to a format suitable for transmission.
  *
  * Note: Once a message has been encoded, it will be ''automatically''
  * released by the Netty codec framework.
  *
  * C.f.[[_root_.io.netty.handler.codec.MessageToByteEncoder#write]]
  */
@ChannelHandler.Sharable
private[netty] class Encoder extends MessageToByteEncoder[SelfDescribing] with Logging {

  import logger._

  /** Called with the outbound message (of type [[fp.model.Message]])
    * that this class will encode to a [[_root_.io.netty.buffer.ByteBuf]].
    *
    * The [[_root_.io.netty.buffer.ByteBuf]] is then forwarded to the next
    * [[_root_.io.netty.channel.ChannelOutboundHandler]] in the pipeline.
    */
  override def encode(ctx: ChannelHandlerContext, sd: SelfDescribing, out: ByteBuf): Unit = {
    trace(s"Encoding message: $sd")
    out.writeBytes(sd.pickle.value.getBytes)
  }

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

  import logger._
  import java.util.{ List => JList }

  /** Called with a [[ByteBuf]] containing incoming data and a List to which
    * decoded messages are added. This call is repeated until it is determined
    * that no new items have been added to the List or no more bytes are readable
    * in the ByteBuf. Then, if the List is not empty, its contents are passed to
    * the next handler in the pipeline.
    */
  override def decode(ctx: NettyContext, in: ByteBuf, out: JList[Object]): Unit = {
    try {

      val buf: ByteBuf = in.readBytes(in.readableBytes())
      val arr: Array[Byte] = if (buf.hasArray()) buf.array() else {
        val bos = new ByteArrayOutputStream
        while (buf.isReadable()) bos.write(buf.readByte())
        bos.toByteArray()
      }

      if (!arr.isEmpty) {
        val received = new String(arr)
        val sd = JSONPickle(received).unpickle[SelfDescribing]
        val msg = sd.unpickleWrapped[Message]
        trace(s"Decoded message: $msg")
        out add msg
      } else () // nop

      buf.release()
    } catch { case e: Throwable =>
        error(s"Decoding error: $e\n${e.getStackTrace.mkString("\n")}")
    }
  }

}

