package silt.samples
package netty
package sandbox

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{ Success, Failure }

import com.typesafe.scalalogging.{ StrictLogging => Logging }

import silt.SiloSystem

object SiloSystemServerMode extends AnyRef with App with Logging {

  /* Run silo system in server mode.
   *
   * In order to shutdown a silo system running in server mode, clients must
   * send the string "shutdown".
   */
  SiloSystem(port = Some(8091))

}

object SiloSystemClientMode extends AnyRef with App with Logging {

  /* Run silo system in client-mode.
   *
   */
  //SiloSystem() match {
  // case Success(system) =>
  //   // create initial silo, i.e., upload initial data
  //   // define and execute your workflow
  //   // close all open connections and terminate silo system
  //   system.terminate()
  // case Failure(error) =>
  //   logger.error(s"Could not instantiate silo system at `localhost`:\n ${error.getMessage}")
  //}

}

object SiloSystemDualMode extends AnyRef with App with Logging {

  /* Run silo system in dual mode.
   *
   * In order to shutdown a silo system running in dual mode, clients may send
   * the string "shutdown", or the silo system must [[terminate
   * SiloSystem#termiante]] itself.
   */
  SiloSystem(port = Some(8091)) match {

    /* At this level correctness of a silo system realization is verified. That
     * is, subtyping correctness re [[impl.SiloSystem]].
     */
    case Success(system) =>
      system onComplete {

        case Success(system) =>
          logger.info(s"Silo system in server mode up and running at `${system.name}`.")

          /* Here it is demonstrated what running a silo system in dual mode
           * means. Despite serving silos, this silo system also defines and
           * executes workflows on those and remotes.
           *
           * XXX Cf. question re "Does it make sense to have a silo system
           * running in dual mode?" at
           * [[https://github.com/normenmueller/f-p/wiki/Understanding-silo-systems]]
           */
          logger.debug(">>> workflow definition and execution")
          for (i <- 1 to 100) {
            logger.debug(f"... working ... ($i%04d)")
            Thread.sleep(100)

            /* XXX What if a client, during this workflow execution, sends a
             * "shutdown" to the underlying server of this silo system running
             * in dual mode? Currently, the server is shutdown but the silo
             * system!
             */
          }

          // JVM does not terminate if the following call is commented out
          system.shutdown()

        case Failure(error) =>
          logger.error(s"Could not start silo system in server mode:\n ${error.getMessage}")
      }
      Thread.sleep(5000)

    case Failure(error) => logger.error(s"Could not instantiate silo system:\n ${error.getMessage}")

  }

}

// vim: set tw=80 ft=scala:
