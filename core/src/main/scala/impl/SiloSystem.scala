package silt
package impl

import java.util.concurrent.CountDownLatch

import scala.collection.concurrent.TrieMap
import scala.concurrent.{ ExecutionContext, Future, Promise }
import ExecutionContext.Implicits.{ global => executor }
import scala.pickling._
import scala.pickling.Defaults._

import com.typesafe.scalalogging.{ StrictLogging => Logging }

/** A Netty-based implementation of a silo system. */
class SiloSystem extends AnyRef with silt.SiloSystem with silt.Internals with Transfer with Logging {

  /* Synchronizer for this silo system.
   *
   * A silo system running in server mode keeps on running until the latch
   * reaches the terminal state. That is, the gate is closed and no thread can
   * pass. In the terminal state the gate opens, allowing all threads to pass,
   * say, to terminate the silo system and the server, respectively.
   */
  protected val hook: Option[CountDownLatch] = None

  // Members declared in silt.impl.Transfer

  override val statusOf = new TrieMap[Host, Status]

  override val promiseOf = new TrieMap[Id, Promise[Response]]

  // Members declared in silt.SiloSystem

  override val name = (new java.rmi.dgc.VMID()).toString

  override def terminate(): Future[Unit] = {
    val promise = Promise[Unit]

    // Close connections TO other silo systems
    logger.trace("Close connctions to other silo systems")
    val to = statusOf collect {
      case (_, Connected(channel, wrkr)) =>
        println("1"); post(channel, Disconnect) andThen {
          case _ =>
            println("2"); channel.closeFuture()
        } andThen {
          case _ =>
            println("3"); wrkr.shutdownGracefully()
        }
    }

    // Close connections FROM other silo systems
    // val from = XXX
    println("4")

    // Terminate underlying server
    // Future.sequence(to /*, from*/ ) onComplete {
    //   case _ =>
    //     println("5")
    //     server map { _.stop() }
    //     println("6")
    //     hook map { _.countDown() }
    //     promise success (())
    // }

    promise.future
  }

  // Members declared in silt.Internals

  //override val processor = ??? // XXX new Processor //(self, new LinkedBlockingQueue[Incoming]())

  override def initiate[R <: silt.Request: Pickler](to: Host)(request: Id => R): Future[silt.Response] =
    connect(to) flatMap { via => send(via, request(id.next)) }

  override def withServer(host: Host): Future[silt.SiloSystem with Server] = {
    /* Promise a silo system, and fulfill this promise with the completed
     * startup of the underlying server. Cf. [[Server#run]].
     */
    val promise = Promise[silt.SiloSystem with Server]
    executor execute (new SiloSystem with Server {

      // silt.SiloSytem w/ internals
      override val name = host.toString

      // silt.Server
      override val at = host

      // silt.impl.SiloSystem
      override val hook = Some(new CountDownLatch(1))

      // silt.impl.Server
      override val started = promise

      (new Thread { override def run(): Unit = hook map (_.await()) }).start()

    })
    promise.future
  }

}

// vim: set tw=80 ft=scala:
