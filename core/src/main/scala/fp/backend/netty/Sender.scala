package fp
package backend
package netty

import scala.collection._
import fp.util.AsyncExecution
import scala.concurrent.{ Future, Promise }
import scala.util.{Failure, Success}
import scala.pickling.{Pickler, Unpickler}

import fp.backend.SelfDescribing
import fp.model._

import io.netty.channel.Channel

trait Sender {
  this: AsyncExecution =>

  def responsesFor: mutable.Map[MsgId, Promise[Response]]

  def wrapBeforeSending[M <: Message: Pickler: Unpickler](msg: M): SelfDescribing =
    SelfDescribing(msg)

  def sendAndForget(via: Channel, wrappedMsg: SelfDescribing): Future[Unit] = {
    val promise = Promise[Unit]
    via.writeAndFlush(wrappedMsg) onComplete {
      case s: Success[_] => promise.success(())
      case Failure(e) => promise.failure(e)
    }
    promise.future
  }

  def sendAndForget[M <: Message: Pickler: Unpickler](via: Channel, msg: M): Future[Unit] =
    sendAndForget(via, wrapBeforeSending(msg))

  def send(via: Channel, id: MsgId, wrappedMsg: SelfDescribing): Future[Response] = {
    val promise = Promise[Response]
    responsesFor += (id -> promise)
    via.writeAndFlush(wrappedMsg)
    promise.future
  }

  def send[M <: Message: Pickler: Unpickler](via: Channel, msg: M): Future[Response] =
      send(via, msg.id, wrapBeforeSending(msg))


}

