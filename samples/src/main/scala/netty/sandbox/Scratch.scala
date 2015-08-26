package silt.samples
package netty
package sandbox

import scala.util.{ Success, Failure }

import com.typesafe.scalalogging.{ StrictLogging => Logging }

import silt.SiloSystem

object Scratch extends AnyRef with App with Logging {

  /* Silo system in server-mode */
  SiloSystem(port = Some(8091)) match {
    case Success(system) =>
      // XXX At this point in time the silo sytem incl. the underlying server
      // must be up and running
      //
      // XXX Silo system info (e.g. via `system.info`)?
      logger.info("Silo system in server-mode up and running at `???`.")

      // do something
      logger.debug(">>> workflow definition")
      for (i <- 1 to 10) {
        logger.debug(f"... working ... ($i%04d)")
        Thread.sleep(100)
      }

      // XXX Make it configurable to let system run forever, till shutdown via a
      // dedicated message to the underlying server or the JVM exits? That is,
      // run the entire silo system as a daemon. Such a silo system would not
      // allow any further interaction, though, but via the underlying server.
      // shutdown system


      // XXX What if termination call is left out? At least within Sbt, the
      // underlying server keeps on running.
      logger.info("Silo system in server-mode is shutting down...")
      system.terminate()
    case Failure(err) =>
      logger.error(s"Could not instantiate silo system at `localhost`:\n ${err.getMessage}")
  }

  /* Silo system in client-mode */
  //SiloSystem() match {
  // case Success(system) =>
  //   // create initial silo, i.e., upload initial data
  //   // define and execute your workflow
  //   // close all open connections and terminate silo system
  //   system.terminate()
  // case Failure(err) =>
  //   logger.error(s"Could not instantiate silo system at `localhost`:\n ${err.getMessage}")
  //}

}

// vim: set tw=80 ft=scala:
