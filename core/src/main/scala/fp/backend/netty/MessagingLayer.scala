package fp
package backend
package netty

import scala.collection.mutable.{Map => Mmap}
import java.net.InetSocketAddress
import java.util.concurrent.PriorityBlockingQueue

import fp.model.{Response, MsgId}

/* This trait will be refactored in the next cycle to make
 * it backend independent and will be placed in fp.backend. */
trait MessagingLayer[Context] {

  type ExpectedMsgId = MsgId
  type OnHoldMessages = PriorityBlockingQueue[WrappedMsg[Context]]
  type MessagingStatus = (ExpectedMsgId, OnHoldMessages)
  type MessageHub = Mmap[InetSocketAddress, MessagingStatus]

  private def defaultStatus: MessagingStatus =
    (MsgId(1), new PriorityBlockingQueue[WrappedMsg[Context]])

  def initMessagingHub: MessageHub =
    Mmap.empty[InetSocketAddress, MessagingStatus]
      .withDefaultValue(defaultStatus)

  /** Keep track of the status of communications from any given node. */
  def statusFrom: MessageHub

  /** Keep track of the last pending message for every host. */
  def unconfirmedResponses: Mmap[InetSocketAddress, Response]

}
