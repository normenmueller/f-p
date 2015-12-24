package silt

import scala.concurrent.Future
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
    * [[silt.impl.SiloSystem]] with a default, empty constructor. The concrete
    * realization is specified by the system property
    * `-Dsilt.system.impl=<class>`. If no system property is given, the
    * realization defaults to [[silt.impl.netty.SiloSystem]].
    *
    * As default, in both cases, server as well as client mode, Netty is used to
    * realize the network layer.
    *
    * @param port network port
    */
  def apply(port: Option[Int] = None): Try[Future[SiloSystem]] = Try {
    val clazz = sys.props.getOrElse("silo.system.impl", "silt.impl.netty.SiloSystem")
    logger.info(s"Initializing silo system with `$clazz`")
    Class.forName(clazz).newInstance().asInstanceOf[impl.SiloSystem]
  } map (_ withServer (port map (Host("127.0.0.1", _))))

}

/** A silo system.
  *
  * On the logical level, a silo system can be understood as the entry point to
  * a collection of silos. On the technical level, it constitutes the interface
  * to the F-P runtime.
  */
trait SiloSystem {

  self: SiloSystemInternal =>

  /** Returns the name of the silo system.
    *
    * If the silo system is running in server mode, [[name]] defaults to `Host @
    * Port`.
    *
    * If the silo system is running is client mode, [[name]] defaults to the
    * respective [[java.rmi.dgc.VMID VMID]].
    */
  def name: String

  /** Terminates the silo system. */
  /* XXX Terminate silo system 
   * - Wait until all connections to/ from this silo system are closed
   * - Timeout
   * - Return type Future[Status]
   */
  def terminate(): Unit

  ///** Uploads a silo to `host` with the initialization process of `clazz`.
  //  *
  //  * Note: `clazz` must provide methods for the silo initialization process.
  //  * This constrain is not yet specified on the type level.
  //  *
  //  * @param clazz logic to initialize the to be created silo
  //  * @param host location of the to be created silo
  //  */
  //def fromClass[U, T <: Traversable[U]](clazz: Class[_], host: Host): Future[SiloRef[U, T]] =
  //  initRequest[U, T, InitSilo](host, {
  //    //println(s"fromClass: register location of $refId")
  //    InitSilo(clazz.getName())
  //  })

}

/* Internal requirements of a silo system.
 *
 * Those internal requirements are basically implementation details to be hidden
 * from the public API. For example, those internals abstract from different
 * network layer back-ends (cf. [[Server]]).
 */
private[silt] trait SiloSystemInternal {

  /* Silo system's underlying server if running in server mode. */
  def server: Option[Server] = None

  import scala.collection.mutable
  import scala.collection.concurrent.TrieMap

  /* Silo locations identified via respective SiloRef */
  def location: mutable.Map[SiloRefId, Host] = new TrieMap[SiloRefId, Host]

}

// vim: set tw=80 ft=scala:
