package fp
package backend
package netty

import fp.model.Message
import fp.util.AsyncExecution
import io.netty.channel.Channel

import scala.concurrent.{Future, Promise}
import scala.pickling.Pickler
import scala.util.{Failure, Success}

trait Helper {
  this: AsyncExecution =>

  // XXX change return type to `Unit` but `sync`?
  def tell[M <: Message: Pickler](via: Channel, message: M): Future[Channel] = {
    val promise = Promise[Channel]
    via.writeAndFlush(message) onComplete {
      case Success(c) => promise.success(c)
      case Failure(e) => promise.failure(e)
    }
    promise.future
  }

}

