package silt
package impl
package netty

import scala.pickling.binary._
import scala.pickling.static._
import scala.pickling.{ Defaults, Pickle, Pickler }
import Defaults.{ pickleOps, stringPickler, unpickleOps }

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.logging.{ LogLevel, LoggingHandler => Logger }
import io.netty.handler.codec.{ ByteToMessageDecoder, MessageToByteEncoder }

import com.typesafe.scalalogging.{ StrictLogging => Logging }

private[netty] class SystemMessageEncoder extends MessageToByteEncoder[silt.Message] with Logging {

  override def encode(ctx: ChannelHandlerContext, msg: silt.Message, out: ByteBuf): Unit = {
    //logger debug s"Encoder received ${msg.payload}"
    //out writeBytes msg.payload.pickle.value
  }

  private def pickle[T <: silt.Message: Pickler](msg: T): Pickle = {
    //val pickler = implicitly[Pickler[T]]
    //val tag     = pickler.tag
    //println(s"tag: ${tag.key}")
    ???
  }

}

import java.util.{ List => JList }

private[netty] class SystemMessageDecoder extends ByteToMessageDecoder with Logging {

  override def decode(ctx: ChannelHandlerContext, in: ByteBuf, out: JList[Object]): Unit = {
    // Read all readable bytes from the input `ByteBuf`
    val buf: ByteBuf = in.readBytes(in.readableBytes())
    val str = buf.toString(io.netty.util.CharsetUtil.US_ASCII).trim()
    logger debug s"Decoder received `$str`"
    out add str
  }

}

// vim: set tw=80 ft=scala:
