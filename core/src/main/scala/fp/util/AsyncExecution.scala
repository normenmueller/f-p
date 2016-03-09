package fp.util

import scala.concurrent.ExecutionContext

/** Piece of cake that allows any class to mix in and get access to
  * an implicit [[ExecutionContext]] for running [[scala.concurrent.Future]]s.
  */
trait AsyncExecution {
  implicit val ec: ExecutionContext
}

