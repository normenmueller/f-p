package fp
package backend

import java.util.concurrent.atomic.AtomicInteger

import com.typesafe.scalalogging.{StrictLogging => Logging}
import fp.model.{ClientRequest, Response}

import scala.concurrent.{ExecutionContext, Future}
import scala.pickling._


/** Logical entry point to a collection of [[Silo]]s -- a Silo manager. */
trait SiloSystem extends Logging {

  implicit val ec: ExecutionContext

  /** Name identifying a given silo system.
    *
    * In server mode, [[name]] defaults to `Host:Port`.
    * Otherwise, [[name]] defaults to a random [[java.util.UUID]].
    */
  def name: String

  /** Terminate the silo system. */
  def terminate(): Future[Unit]

  def request[R <: ClientRequest: Pickler](at: Host)(request: MsgId => R): Future[Response]

  object MsgIdGen {
    private lazy val ids = new AtomicInteger(10)
    def next = MsgId(ids.incrementAndGet())
  }

}

/** Exposes constructors to create the user either a [[SiloSystem]] with
  * either server or client capabilities.
  *
  * Must be extended by the companion object of a [[SiloSystem]] for every
  * possible backend. For instance, netty-based and akka-based implementations.
  */
trait SiloSystemCompanion extends Logging {

  /** Return a [[SiloSystem]] running in server mode if port is not [[None]].
    * Otherwise, return a [[SiloSystem]] in client mode.
    *
    * @param port Port number
    */
  def apply(port: Option[Int] = None): Future[SiloSystem]

  /** Return a server-based silo system.
    *
    * A silo system running in server mode has an underlying [[fp.backend.Server]]
    * to host silos and make those available to other silo systems.
    *
    * The underlying server is private to the silo system, i.e., only the silo
    * system itself directly communicates with the server. A user/client only
    * directly communicates with such a silo system.
    *
    * @param host Host where the server will be booted up
    */
  def apply(host: Host): Future[SiloSystem]

}
