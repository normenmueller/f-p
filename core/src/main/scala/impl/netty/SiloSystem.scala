package silt
package impl
package netty

import java.util.concurrent.{ BlockingQueue, LinkedBlockingQueue }

import scala.concurrent.ExecutionContext
import scala.concurrent.{ Await, Future, Promise }
import scala.util.Try

import com.typesafe.scalalogging.{ StrictLogging => Logging }

/** A Netty-based implementation of a silo system.
  *
  * @param server underlying server for hosting silos
  */
class SiloSystem(server: Option[Server]) extends AnyRef with impl.SiloSystem with Logging {

  // for reflective instantiation
  def this() = this(None)

  server map { runnable =>
    val executor: ExecutionContext = ExecutionContext.global
    executor execute runnable
  }

  // the queue the silo system rsp. the silo system's receptor is working on.
  private val queue: BlockingQueue[Incoming] = new LinkedBlockingQueue[Incoming]()

  override def withServer(at: Option[Host]): Try[SiloSystem] = Try {
    at match {
      case None       => this // XXX run in client mode
      case Some(host) => new SiloSystem(Some(new Server(host, queue)))
    }
  }

  override def terminate(): Unit = {
    logger.debug("Silo system terminates underlying server...")
    // XXX re-assess alternative to send specific, internal termination message to server
    server map (_.stop)
  }

}

// vim: set tw=80 ft=scala:
