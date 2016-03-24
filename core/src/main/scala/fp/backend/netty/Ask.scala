package fp
package backend
package netty

import scala.collection._
import fp.util.AsyncExecution
import scala.concurrent.{ Future, Promise }
import scala.util.{Failure, Success}
import scala.pickling.{Pickler, Unpickler}

import fp.backend.SelfDescribing
import fp.model.{ClientRequest, Response, MsgId}

import io.netty.channel.Channel

trait Ask {
  this: AsyncExecution =>

  def responsesFor: mutable.Map[MsgId, Promise[Response]]

  /** Send a message to a node to start a conversation.
    * As usual, a conversation starts with only a request.
    */
  def ask[R <: ClientRequest: Pickler: Unpickler]
      (via: Channel, request: R): Future[Response] = {

    val promise = Promise[Response]

    val wrapped = SelfDescribing(request)
    responsesFor += (request.id -> promise)
    via.writeAndFlush(wrapped)

    promise.future

  }

}

