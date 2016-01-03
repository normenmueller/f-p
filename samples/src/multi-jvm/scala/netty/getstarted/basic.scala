package silt.samples
package netty

import scala.util.{ Success, Failure }
import com.typesafe.scalalogging.{ StrictLogging => Logging }

/**
 * `Node1` can be understood as a slave, worker, workhorse, or executor. Choose
 * according to what suits you best.
 * 
 * All these terms have their raison d'être anyway, i.e., all these terms, in
 * general, state the fact that this node is for hosting silos and thus for
 * executing computations defined and shipped by some other node --- here, in
 * this example, that other node is `Node2`.
 * 
 * To allow for creation of silos by other nodes, the F-P runtime requires a
 * web server. Current default is a netty-based web server. 
 */
object BasicMultiJvmNode1 extends Logging {

  import silt._

  def main(args: Array[String]): Unit = 
    SiloSystem(port = Some(8090)) match {
      case Success(sys) => logger.info("Node1 up and running.")
      case Failure(err) => logger.error(s"Could not instantiate silo system at `Node1`:\n ${err.getMessage}")
    }

}

/**
 * `Node2` can be understood as master, or driver.
 * 
 * All these terms have their raison d'être, i.e., all these terms, in general,
 * state the fact that this node is for defining the control flow, the to be
 * executed computations and for sending those computations to respective
 * slaves or executors.
 */ 
object BasicMultiJvmNode2 {

  def main(args: Array[String]): Unit = { 
    Thread.sleep(1000) // XXX wait for Node1
  }
 
}

// vim: set tw=80 ft=scala:
