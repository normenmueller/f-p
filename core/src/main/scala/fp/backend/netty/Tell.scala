package fp
package backend
package netty

import fp.model.Message
import fp.util.AsyncExecution
import io.netty.channel.Channel

import scala.concurrent.{Future, Promise}
import scala.pickling.{Pickler, Unpickler}
import scala.util.{Failure, Success}

trait Tell {
  this: AsyncExecution =>

  /** Send a message to a node with which we have
    * already established a channel. Usually used
    * for both sending requests and responses.
    *
    * @param via The channel
    * @param msg What we tell to the [[Server]]
    * @tparam M Type of message
    *
    * TODO change return type to `Unit` but `sync`?
    */
  def tell[M <: Message: Pickler: Unpickler]
      (via: Channel, msg: M): Future[Channel] = {

    val promise = Promise[Channel]
    val wrapped = SelfDescribing(msg)
    via.writeAndFlush(wrapped) onComplete {
      case Success(c) => promise.success(c)
      case Failure(e) => promise.failure(e)
    }
    promise.future

  }

}

