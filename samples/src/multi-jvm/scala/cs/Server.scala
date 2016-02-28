package multijvm
package cs

import scala.concurrent.{ Await, ExecutionContext, Future, Promise }
import scala.concurrent.duration._
import ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{ Success, Failure }

import com.typesafe.scalalogging.{ StrictLogging => Logging }

import fp._

/** A silo system running in server mode can be understood as a ''slave'',
 *  ''worker'', ''workhorse'', or ''executor''. You name it!
  *
  * All these terms have their raison d'être, i.e., all these terms, in general,
  * state the fact that this node is for hosting silos and thus for executing
  * computations defined and shipped by some other node --- here, in this
  * example, that other node is [[Clients]].
  *
  * To allow for creation of silos by other nodes, the F-P runtime requires a
  * web server. Current default is a Netty-based web server.
  */
object Server extends AnyRef with Logging {

  import logger._

  def main(args: Array[String]): Unit =
    Await.ready(SiloSystem(port = Some(8090)), 10.seconds) onComplete {
      case Success(sys) =>
        info(s"Silo system `${sys.name}` up and running.")
      case Failure(err) =>
        error(s"Could not start silo system in server mode: ${err.getMessage}")
    }

}
