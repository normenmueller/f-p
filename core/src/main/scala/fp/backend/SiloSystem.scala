package fp
package backend

import fp.model.{SiloSystemId, ClientRequest, Response, MsgId}
import fp.util.{AsyncExecution, IntGen, Gen}

import scala.pickling._
import scala.concurrent.Future
import com.typesafe.scalalogging.{StrictLogging => Logging}


/** Represents a node in a network that knows how to interact with
  * other nodes running the function-passing model. It also provides
  * to the programmer an entry point to manage [[Silo]]s.
  */
trait SiloSystem extends AsyncExecution with Logging {

  /** Identify a given silo system.
    *
    * In server mode, [[systemId]] defaults to `host:port`.
    * Otherwise, [[systemId]] defaults to a random [[java.util.UUID]].
    */
  def systemId: SiloSystemId

  /** Terminate the silo system. */
  def terminate(): Future[Unit]

  def request[R <: ClientRequest: Pickler: Unpickler]
    (at: Host)(request: MsgId => R): Future[Response]

  object MsgIdGen extends Gen[MsgId] {
    object IntGen extends IntGen
    def next = MsgId(IntGen.next)
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
