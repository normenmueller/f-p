package silt
package impl

import scala.pickling._
import scala.pickling.Defaults._

import _root_.io.netty.buffer.ByteBuf
import _root_.io.netty.channel.ChannelHandlerContext
import _root_.io.netty.handler.logging.{ LogLevel, LoggingHandler => Logger }
import _root_.io.netty.handler.codec.{ ByteToMessageDecoder, MessageToByteEncoder }

import _root_.com.typesafe.scalalogging.{ StrictLogging => Logging }

//import binary._
import json._

/** Encoder converts F-P system messages to a format suitable for transmission.
  */
private[impl] class SystemMessageEncoder extends MessageToByteEncoder[silt.Message] with Logging {

  /* Called with the outbound message (of type [[silt.Message]]) that this class
   * will encode to a [[_root_.io.netty.buffer.ByteBuf]]. The
   * [[_root_.io.netty.buffer.ByteBuf]] is then forwarded to the next
   * [[_root_.io.netty.channel.ChannelOutboundHandler]] in the pipeline.
   */
  override def encode(ctx: ChannelHandlerContext, msg: silt.Message, out: ByteBuf): Unit = {
    logger.trace(s"Encoder for silo system messages received:\n$msg")
    out.writeBytes(pickle(msg))
  }

  def pickle[T <: silt.Message: Pickler](msg: T): Array[Byte] = {
    val pickler = implicitly[Pickler[T]]
    val tag = pickler.tag
    val builder = pickleFormat.createBuilder()

    logger.trace(s"Pickler tag: ${tag.key}")
    logger.trace(s"Pickler class: ${pickler.getClass.getName}")

    builder.hintTag(tag)
    pickler.pickle(msg, builder)

    val picklee = builder.result().value
    logger.trace(s"Picklee:\n$picklee")

    picklee.getBytes
  }

}

import java.util.{ List => JList }

/** Decoder converts a network stream, encoded by [[SystemMessageEncoder]] back
  * to the F-P system message format.
  */
private[impl] class SystemMessageDecoder extends ByteToMessageDecoder with Logging {

  /* Called with a [[ByteBuf]] containing incoming data and a List to which
   * decoded messages are added. This call is repeated until it is determined
   * that no new items have been added to the List or no more bytes are readable
   * in the ByteBuf. Then, if the List is not empty, its contents are passed to
   * the next handler in the pipeline.
   *
   * Note: once a message has been decoded, it will ''automatically'' be
   * released by a call to ReferenceCountUtil.release(message)
   */
  override def decode(ctx: ChannelHandlerContext, in: ByteBuf, out: JList[Object]): Unit = {
    // Read all readable bytes from the input `ByteBuf`
    val buf: ByteBuf = in.readBytes(in.readableBytes())

    val str = buf.toString(_root_.io.netty.util.CharsetUtil.US_ASCII).trim()
    logger.debug(s"Decoder received `$str`")
    val command = str.unpickle[silt.Request]

    //val arr: Array[Byte] = buf.array()
    //val pickle = BinaryPickle(arr)
    //val command = pickle.unpickle[silt.Response]
    println(s"CLIENT: received $command")

    out add command
  }

}

// vim: set tw=80 ft=scala:
