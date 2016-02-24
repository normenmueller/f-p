package fp
package backend
package netty

import scala.collection._
import scala.concurrent.{ ExecutionContext, Future, Promise }
import scala.pickling.Pickler
import scala.util.{ Failure, Success }

import _root_.io.netty.channel.Channel

trait Ask {

  import ExecutionContext.Implicits.global

  def promiseOf: mutable.Map[MsgId, Promise[Response]]

  def ask[R <: fp.RSVP: Pickler](via: Channel, request: R): Future[Response] = {
    val promise = Promise[Response]

    promiseOf += (request.id -> promise)
    via.writeAndFlush(request)

    promise.future
  }

}

