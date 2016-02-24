package fp.samples
package netty
package sandbox

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{ Success, Failure }

import com.typesafe.scalalogging.{ StrictLogging => Logging }

import fp.SiloSystem

object SiloSystemServerMode extends AnyRef with App with Logging {

  /* Run silo system in server mode.
   */
  Await.result(SiloSystem(port = Some(8091)), 10.seconds)

}

object SiloSystemClientMode extends AnyRef with App with Logging {

  /* Run silo system in client-mode.
   */
  val system = Await.result(SiloSystem(), 10.seconds)

  logger.info(s"Silo system `${system.name}` up and running.")
  logger.info(s"Initiating termination of silo system `${system.name}`...")
  Await.result(system.terminate(), 10.seconds)
  logger.info(s"Silo system `${system.name}` terminated.")

}

object SiloSystemDualMode extends AnyRef with App with Logging {

  /* Run silo system in dual mode.
   *
   * In order to shutdown a silo system running in dual mode
   * - clients may send the message [[Terminate]], or
   * - the silo system must [[terminate SiloSystem#terminate]] itself.
   */
  Await.ready(SiloSystem(port = Some(8091)), 10.seconds) onComplete {
    case Success(system) =>
      logger.info(s"Silo system `${system.name}` up and running at.")

      /* Here it is demonstrated what running a silo system in dual mode
       * means. Despite serving silos, this silo system also defines and
       * executes workflows on those and remotes.
       *
       * XXX Cf. question re "Does it make sense to have a silo system
       * running in dual mode?" at
       * [[https://github.com/normenmueller/f-p/wiki/Understanding-silo-systems]]
       */
      logger.debug(">>> workflow definition and execution")
      for (i <- 1 to 50) {
        logger.debug(f"... working ... ($i%04d)")
        Thread.sleep(100)

        /* XXX What if a client, during this workflow execution, sends a
         * "shutdown" to the underlying server of this silo system running
         * in dual mode? Currently, the server is shutdown but the silo
         * system!
         */
      }

      // JVM does not terminate if the following call is commented out
      try {
        Await.result(system.terminate(), 2.seconds)
        logger.info(s"Silo system `${system.name}` terminated.")
      } catch {
        case err: Throwable =>
          println(err.getMessage())
          sys.exit(1)
      }

    case Failure(error) => logger.error(s"Could not start silo system in server mode:\n ${error.getMessage}")
  }

}

// vim: set tw=80 ft=scala:
