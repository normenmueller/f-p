package silt
package impl

import io.netty.channel.ChannelHandlerContext

package object netty {

  private[netty] sealed abstract class Message
  private[netty] case class Incoming(ctx: ChannelHandlerContext, msg: Any) extends Message

}

// vim: set tw=80 ft=scala:
