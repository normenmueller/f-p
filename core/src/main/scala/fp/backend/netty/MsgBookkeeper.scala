package fp
package backend
package netty

import scala.collection.mutable.{Map => Mmap}
import java.net.InetSocketAddress
import java.util.concurrent.PriorityBlockingQueue

import fp.model.{Response, Message, MsgId}

/* This trait will be refactored in the next cycle to make
 * it backend independent and will be placed in fp.backend. */
trait MsgBookkeeper {

  type ExpectedMsgId = MsgId

  /* This priority blocking queue will be replaced by a lock-free priority queue
   * that @jvican is implementing for Scala, since this one is blocking. */
  type MsgBookkeeping[C] = (ExpectedMsgId, PriorityBlockingQueue[WrappedMsg[C]])
  type NettyBookkeeper = Mmap[InetSocketAddress, MsgBookkeeping[NettyContext]]

  object MsgBookkeeping {
    def default[C]: MsgBookkeeping[C] =
      (MsgId(1), new PriorityBlockingQueue[WrappedMsg[C]])
  }

  /** Keep track of the status of communications from any given node. */
  def statusFrom: NettyBookkeeper

  def initMsgBookkeeping: NettyBookkeeper =
    Mmap[InetSocketAddress, MsgBookkeeping[NettyContext]]()
      .withDefaultValue(MsgBookkeeping.default[NettyContext])

  /** Keep track of the last pending message for every host. */
  def pendingOfConfirmation: Mmap[InetSocketAddress, Response]

}
