package silt
package impl
package netty

import scala.concurrent.{ ExecutionContext, Future, Promise }
import scala.pickling.Pickler
import scala.util.{ Failure, Success }

import _root_.io.netty.channel.Channel

trait Tell {

  import ExecutionContext.Implicits.global

  // XXX change return type to `Unit` but `sync`?
  def tell[M <: silt.Message: Pickler](via: Channel, message: M): Future[Channel] = {
    val promise = Promise[Channel]

    via.writeAndFlush(message) onComplete {
      case Success(c) => promise.success(c)
      case Failure(e) => promise.failure(e)
    }

    promise.future
  }

}

