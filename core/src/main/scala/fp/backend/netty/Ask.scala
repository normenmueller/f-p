package fp
package backend
package netty

import fp.model.MsgId
import io.netty.channel.Channel
import model.{ ClientRequest, Response }

import scala.collection._
import scala.concurrent.{ Future, Promise }
import scala.pickling.Pickler

trait Ask {

  def responsesFor: mutable.Map[MsgId, Promise[Response]]

  def ask[R <: ClientRequest: Pickler](via: Channel, request: R): Future[Response] = {
    val promise = Promise[Response]

    responsesFor += (request.id -> promise)
    via.writeAndFlush(request)

    promise.future
  }

}

