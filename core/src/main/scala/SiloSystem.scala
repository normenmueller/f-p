package silt

import java.util.concurrent.atomic.AtomicInteger

import scala.collection._
import scala.collection.concurrent.TrieMap
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.pickling.Pickler
import scala.util.{ Try, Success, Failure }

import com.typesafe.scalalogging.{ StrictLogging => Logging }

/** Provides a set of operations needed to create [[SiloSystem]]s. */
object SiloSystem extends AnyRef with Logging {

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
    * [[silt.impl.SiloSystem]] with a default, empty constructor. The concrete
    * realization is specified by the system property `-Dsilo.system.impl=<class>`.
    * If no system property is given, the realization defaults to
    * [[silt.impl.netty.SiloSystem]].
    *
    * In both server and client mode, Netty is used to realize the network layer.
    *
    * @param port Network port
    */
  def apply(port: Option[Int] = None): Future[SiloSystem] = Future {
    val clazz = sys.props.getOrElse("silo.system.impl", "silt.impl.netty.SiloSystem")
    logger.info(s"Initializing silo system with `$clazz`")
    Class.forName(clazz).newInstance().asInstanceOf[silt.impl.SiloSystem]
  } flatMap { system =>
    port match {
      case None       => Future.successful(system)
      case Some(port) => system withServer Host("127.0.0.1", port)
    }
  }

}

/** A silo system is a logical entry point to a collection of [[Silo]]. */
trait SiloSystem extends SiloRefFactory with Logging {

  self: Internals =>

  /** Name identifying a given silo system.
    *
    * In server mode, [[name]] defaults to `Host:Port`.
    * Otherwise, [[name]] defaults to a random [[java.util.UUID]].
    */
  def name: String

  /** Terminate the silo system.
    *
    * Blocking operation since it waits for all connections to be closed.
    */
  def terminate(): Future[Unit]

  override def populate[T](at: Host)(fun: () => Silo[T])
    (implicit pickler: Pickler[Populate[T]]): Future[SiloRef[T]] = {
    request(at) { msgId => Populate(msgId, fun) } map {
      case Populated(_, ref) => new Materialized[T](ref, at)(self)
      case _ => throw new Exception(s"Silo population at `$at` failed.")
    }
  }

}

/* Represents the internals of a [[SiloSystem]].
 *
 * These internal are implementation details to be hidden from the public API.
 * This class is meant to be an abstraction for different backend (cf. [[Server]]).
 */
private[silt] trait Internals {

  // Send request `request` to `to`.
  def request[R <: silt.RSVP: Pickler](at: Host)(request: MsgId => R): Future[Response]

  // Generator for system message Ids
  object msgId {

    private val ids = new AtomicInteger(10)

    def next = MsgId(ids.incrementAndGet())

  }

}

