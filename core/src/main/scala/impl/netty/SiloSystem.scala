package silt
package impl
package netty

import java.util.concurrent.{ BlockingQueue, LinkedBlockingQueue }

import scala.language.postfixOps
import scala.concurrent.{ Await, ExecutionContext, Future, Promise }
import ExecutionContext.Implicits.{ global => executor }

import com.typesafe.scalalogging.{ StrictLogging => Logging }

/** A Netty-based implementation of a silo system. */
private[silt] class SiloSystem () extends AnyRef with impl.SiloSystem with Logging {

  override val name = (new java.rmi.dgc.VMID()).toString

  // The server if silo system is running in server mode.
  val server: Option[Server] = None

  // Mailbox the silo system rsp. the silo system's receptor is working on.
  val mb: BlockingQueue[Incoming] = new LinkedBlockingQueue[Incoming]()

  override def withServer(at: Option[Host]): Future[SiloSystem] = at match {
    case None       => Future.successful(this) // XXX run in client mode
    case Some(host) => 
      /* Promise a silo system, and fufill this promise with the completed
       * startup of the underlying server. Cf. [[Server#run]].
       */
      val started = Promise[SiloSystem]
      executor execute new SiloSystem() with Server {
        override val server: Option[Server] = Some(this)
        override val name = host.toString
        override val at = host
        override val mq = mb
        override val up = started
      }
      started.future
  }

  override def terminate(): Unit =
    server map { server => 
      logger.debug("Silo system stops underlying server...")
      // XXX re-assess sending specific, internal termination message to server
      server.stop
    }

}

// vim: set tw=80 ft=scala:
