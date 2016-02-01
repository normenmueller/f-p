package silt

import java.util.concurrent.atomic.AtomicInteger

import scala.collection._
import scala.collection.concurrent.TrieMap
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.pickling._
import scala.util.{ Try, Success, Failure }

import com.typesafe.scalalogging.{ StrictLogging => Logging }

/** This object provides a set of operations needed to create [[SiloSystem]]
  * values.
  */
object SiloSystem extends AnyRef with Logging {

  /** Instantiate a silo system.
    *
    * In case of some port is given, the silo system runs in server mode.
    * That is, the silo system is extended by an underlying server
    * located at `localhost` listening at port `port`. The underlying server is
    * required to host silos and make those available to other silo systems.
    *
    * In case of none port is given, the silo system runs in client mode.
    *
    * The actual silo system implementation must be a subclass of
    * [[silt.SiloSystem]] with a default, empty constructor. The concrete
    * realization is specified by the system property
    * `-Dsilo.system.impl=<class>`. If no system property is given, the
    * realization defaults to [[silt.impl.SiloSystem]].
    *
    * As default, in both cases, server as well as client mode, Netty is used to
    * realize the network layer.
    *
    * @param port network port
    */
  //format: OFF
  def apply(port: Option[Int] = None): Future[SiloSystem] = Future {
    val clazz = sys.props.getOrElse("silo.system.impl", "silt.impl.SiloSystem")
    logger.info(s"Initializing silo system with `$clazz`")
    Class.forName(clazz).newInstance().asInstanceOf[silt.SiloSystem with silt.Internals]
  } flatMap { system => port match {
    case None       => Future.successful(system)
    case Some(port) => system withServer Host("127.0.0.1", port)
  } }
  //format: ON

}

/** A silo system.
  *
  * On the logical level, a silo system can be understood as the entry point to
  * a collection of silos. On the technical level, it constitutes the interface
  * to the F-P runtime.
  */
trait SiloSystem extends SiloRefFactory with Logging /* XXX TESTING */ with silt.impl.Transfer /* XXX */ {

  self: Internals =>

  /** Returns the name of the silo system.
    *
    * If the silo system is running in server mode, [[name]] defaults to
    * `Host:Port`.
    *
    * If the silo system is running is client mode, [[name]] defaults to the
    * respective [[java.rmi.dgc.VMID VMID]].
    */
  def name: String

  /** Terminate the silo system. */
  def terminate(): Future[Unit]

  // Members declared in silt.SiloRefFactory

  override final def populate[T](fac: SiloFactory[T])(at: Host)(implicit pickler: Pickler[Populate[T]]): Future[SiloRef[T]] =
    initiate(at) { id => Populate(id, fac) } map {
      case reply: Populated =>
        logger.debug(s"`$name` received from `$at` message `Populated` with message Id `${reply.id}` and correlation Id `${reply.cor}`.")
        ???
      case _ => throw new Exception(s"Silo population at `$at` failed.")
    }

}

/* Internals of a silo system.
 *
 * Those internal requirements are basically implementation details to be hidden
 * from the public API. For example, those internals abstract from different
 * network layer back-ends (cf. [[Server]]).
 */
private[silt] trait Internals {

  /* Return an implementation agnostic silo system running in server mode.
   * @param at [[Host network host]]
   */
  def withServer(at: Host): Future[silt.SiloSystem with Server]

  /* System message processor. */
  //def processor: Processor

  def initiate[R <: silt.Request: Pickler](to: Host)(request: Id => R): Future[silt.Response]

  ///* Send a request */
  //// XXX Return `Future[SelfDesribing]`?
  //def send[R <: silt.Request : Pickler](to: Host, request: R): Future[silt.Response]

  /* Set of addressable silos */
  // XXX With `Id` still required?
  //def location: mutable.Map[Id, Unit] = new TrieMap[Id, Unit]

  val refId = new AtomicInteger(0)

  object id {

    private val ids = new AtomicInteger(10)

    def next = Id(ids.incrementAndGet())

  }

}

// vim: set tw=80 ft=scala:
