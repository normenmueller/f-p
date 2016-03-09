package fp
package backend

import java.util.concurrent.atomic.AtomicInteger

import com.typesafe.scalalogging.{StrictLogging => Logging}
import fp.model.{ClientRequest, Response}

import scala.concurrent.Future
import scala.pickling._


/** Logical entry point to a collection of [[Silo]]s -- a Silo manager. */
trait SiloSystem extends Logging {

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

  /** Instantiate a silo system.
    *
    * If `port` is `None`, the silo system runs in server mode.
    * Otherwise, the silo system runs in client mode.
    *
    * In Server mode, the silo system is extended by an underlying server
    * located at `localhost` listening at `port`. The underlying server
    * is required to host silos and make them available to other silo systems.
    *
    *
    * The actual silo system implementation must be a subclass of
    * [[fp.backend.SiloSystem]] with a default, empty constructor. The concrete
    * realization is specified by the system property `-Dfp.backend=<class>`.
    * If no system property is given, the realization defaults to
    * [[fp.backend.netty.SiloSystem]].
    *
    * In both server and client mode, Netty is used to realize the network layer.
    *
    * @param port Network port
    */
  def apply(port: Option[Int] = None): Future[SiloSystem]

}
