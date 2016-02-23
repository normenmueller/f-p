package my

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.pickling._
import scala.util.{ Success, Failure }
import silt._

/** Test silo system implementation requirements in case of one wants to
  * actually roll his own silo system.
  */
//class SiloSystem extends AnyRef with silt.impl.Requirements {
//
//  // Members declared in silt.SiloSystem
//  
//  def name: String = ???
//  def terminate(): Future[Terminated] = ???
//
//  // Members declared in silt.Internals
//
//  def send[R <: silt.Request: Pickler](to: Host, request: R): Future[silt.Response] = ???
//
//  // Members declared in silt.Hostable
//
//  def withServer(at: silt.Host): Future[SiloSystem with Server] = ???
//}
//
//object Main extends AnyRef with App {
//
//    sys.props.put("silo.system.impl", "my.SiloSystem")
//
//    val system = SiloSystem() match {
//      case Success(system) => scala.concurrent.Await.result(system, 10.seconds)
//      case Failure(error)  => sys.error(s"Could not instantiate silo system:\n ${error.getMessage}")
//    }
//
//}
