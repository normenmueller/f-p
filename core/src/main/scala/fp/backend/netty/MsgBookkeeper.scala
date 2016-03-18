package fp
package backend
package netty

import java.net.InetSocketAddress
import java.util.concurrent.PriorityBlockingQueue

import fp.model.{Response, Message, MsgId}

trait MsgBookkeeper {

  type ExpectedMsgId = MsgId
  /* This blocking queue will be replaced by a lock-free priority queue
   * that @jvican is implementing for Scala, since this one is blocking. */
  type MsgBookkeeping[C] = (ExpectedMsgId, PriorityBlockingQueue[WrappedMsg[C]])

  trait MsgOrdering extends Ordering[Message] {
    def compare(m1: Message, m2: Message): Int =
      Ordering.Int.compare(m2.id.value, m1.id.value)
  }

  implicit object MsgOrdering extends MsgOrdering

  import scala.collection.mutable

  /** Keep track of the last received `MsgId` from a given node. */
  def msgsFrom: mutable.Map[InetSocketAddress, MsgBookkeeping[NettyContext]]

  /** Keep track of the last pending message for every host. */
  def pendingOfConfirmation: mutable.Map[InetSocketAddress, Response]

}
