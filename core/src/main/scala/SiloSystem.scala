package silt

import java.util.concurrent.atomic.AtomicInteger

import scala.collection._
import scala.collection.concurrent.TrieMap
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.pickling.Pickler
import scala.util.{ Try, Success, Failure }

import com.typesafe.scalalogging.{ StrictLogging => Logging }

/** This object provides a set of operations needed to create [[SiloSystem]] values. */
object SiloSystem extends AnyRef with Logging {

  /** Instantiate a silo system.
    *
    * In case of some port is given, the silo system runs in server mode. That is, the silo system is extended by an
    * underlying server located at `localhost` listening at port `port`. The underlying server is required to host silos
    * and make those available to other silo systems.
    *
    * In case of none port is given, the silo system runs in client mode.
    *
    * The actual silo system implementation must be a subclass of [[silt.impl.SiloSystem]] with a default, empty
    * constructor. The concrete realization is specified by the system property `-Dsilo.system.impl=<class>`. If no
    * system property is given, the realization defaults to [[silt.impl.netty.SiloSystem]].
    *
    * As default, in both cases, server as well as client mode, Netty is used to realize the network layer.
    *
    * @param port network port
    */
  def apply(port: Option[Int] = None): Future[SiloSystem] = Future {
    val clazz = sys.props.getOrElse("silo.system.impl", "silt.impl.netty.SiloSystem")
    logger.info(s"Initializing silo system with `$clazz`")
    Class.forName(clazz).newInstance().asInstanceOf[silt.impl.SiloSystem]
  } flatMap { system => port match {
    case None       => Future.successful(system)
    case Some(port) => system withServer Host("127.0.0.1", port)
  } }

}

/** A silo system.
  *
  * On the logical level, a silo system can be understood as the entry point to a collection of silos. On the technical
  * level, it constitutes the interface to the F-P runtime.
  */
trait SiloSystem extends SiloRefFactory with Logging {

  self: Internals =>

  /** Returns the name of the silo system.
    *
    * If the silo system is running in server mode, [[name]] defaults to `Host:Port`. If the silo system is running is
    * client mode, [[name]] defaults to a randomly generated [[java.util.UUID]].
    */
  def name: String

  /** Terminate the silo system.
    *  
    * Use `Await.result(terminate, Duration.Inf)` to wait for all connections being closed.
    */
  def terminate(): Future[Unit]

  override def populate[T](at: Host)(fun: () => Silo[T])(implicit pickler: Pickler[Populate[T]]): Future[SiloRef[T]] =
    request(at) { msgId => Populate(msgId, fun) } map {
      case Populated(_, ref) => new Materialized[T](ref, at)(self)
      case _                 => throw new Exception(s"Silo population at `$at` failed.")
    }

}

/* Internals of a silo system.
 *
 * Those internal requirements are basically implementation details to be hidden from the public API. For example, those
 * internals abstract from different network layer back-ends (cf. [[Server]]).
 */
private[silt] trait Internals {

  // Send request `request` to `to`.
  def request[R <: silt.RSVP : Pickler](at: Host)(request: MsgId => R): Future[silt.Response]

  // Generator for system message Ids
  object msgId {

    private val ids = new AtomicInteger(10)

    def next = MsgId(ids.incrementAndGet())

  }

}

// vim: set tw=120 ft=scala:
