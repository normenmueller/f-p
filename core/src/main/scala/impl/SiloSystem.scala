package silt
package impl

import scala.collection.concurrent.TrieMap
import scala.concurrent.{ ExecutionContext, Future, Promise }
import ExecutionContext.Implicits.{ global => executor }
import scala.pickling._
import scala.pickling.Defaults._

import com.typesafe.scalalogging.{ StrictLogging => Logging }

/** A Netty-based implementation of a silo system. */
class SiloSystem extends AnyRef with silt.SiloSystem with silt.Internals with Transfer with Logging {

  import logger._

  // Members declared in silt.impl.Transfer

  override val statusOf = new TrieMap[Host, Status]

  override val promiseOf = new TrieMap[Id, Promise[Response]]

  // Members declared in silt.SiloSystem

  override val name = (new java.rmi.dgc.VMID()).toString

  override def terminate(): Future[Unit] = {
    val promise = Promise[Unit]

    info(s"Silo system `$name` terminating...")
    //format: OFF
    val to = statusOf collect { case (host, Connected(channel, worker)) =>
      trace(s"Closing connection to `$host`.")
      post(channel, Disconnect)
        .andThen { case _ => worker.shutdownGracefully() }
        .andThen { case _ => statusOf += (host -> Disconnected) }
    }
    //format: ON

    // Close connections FROM other silo systems
    // val from = XXX

    // Terminate underlying server
    //format: OFF
    Future.sequence(to /*, from*/ ) onComplete { case _ =>
      promise success (this match { case server: Server => server.stop() case _ => () })
      info(s"Silo system `$name` terminating done.")
    }
    //format: ON

    promise.future
  }

  // Members declared in silt.Internals

  //override val processor = ??? // XXX new Processor //(self, new LinkedBlockingQueue[Incoming]())

  override def initiate[R <: silt.Request: Pickler](to: Host)(request: Id => R): Future[silt.Response] =
    connect(to) flatMap { via => send(via, request(id.next)) }

  override def withServer(host: Host): Future[silt.SiloSystem with Server] = {
    val promise = Promise[silt.SiloSystem with Server]
    executor execute (new SiloSystem with Server {
      override val name = host.toString
      override val at = host
      override val started = promise
    })
    promise.future
  }

}

// vim: set tw=80 ft=scala:
