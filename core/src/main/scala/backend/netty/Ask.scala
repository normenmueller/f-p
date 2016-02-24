package fp
package impl
package netty

import _root_.io.netty.channel.Channel
import model.{ ClientRequest, Response }

import scala.collection._
import scala.concurrent.{ Future, Promise }
import scala.pickling.Pickler

trait Ask {

  def promiseOf: mutable.Map[MsgId, Promise[Response]]

  def ask[R <: ClientRequest: Pickler](via: Channel, request: R): Future[Response] = {
    val promise = Promise[Response]

    promiseOf += (request.id -> promise)
    via.writeAndFlush(request)

    promise.future
  }

}

