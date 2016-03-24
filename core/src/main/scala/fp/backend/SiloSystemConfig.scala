package fp.backend

import scala.concurrent.{Promise, Future, ExecutionContext}
import scala.util.{Success, Failure}

import org.slf4j.Logger

/* The code here is a proof of concept and it may need some changes
 * before being production-ready. E.g. allow extensibility by removing
 * the sealed keyword for `Setting` and consequent subclasses. */

/** A general configuration for a [[SiloSystem]]
  * gathering all the different settings.
  */
abstract class SiloSystemConfig {

  val retryPolicy: RetryPolicy

}

/** A setting represents a configurable parameter that
  * modifies the internal behavior of the system.
  */
sealed trait Setting

/** Sets the strategy to follow when a connection can not
  * be established or the previous channel has been closed.
  */
sealed trait RetryPolicy extends Setting {

  /** Repeat the execution of an asynchronous side-effect free thunk. */
  def repeat[T](f: () => Future[T])
               (implicit ec: ExecutionContext, lg: Logger): Future[T]

}

case class Repeat(timeout: Long, count: Long) extends RetryPolicy {

  class FailedRetryException(msg: String) extends Exception(msg)

  object FailedRetryException {
    def apply(): FailedRetryException =
      new FailedRetryException("Limit of retries has been reached.")
  }

  def repeat[T](thunk: () => Future[T])
               (implicit ec: ExecutionContext, lg: Logger) = {

    /* This **can't** be tail recursive */
    def times(n: Long): Future[T] = {
      if (n > count) Future.failed(FailedRetryException())
      else {
        thunk() recoverWith {
          case ex: Exception =>
            val m = n + 1
            lg.warn(s"Retry #$m failed:\n\t$ex")
            times(m)
        }
      }
    }

    times(0)
  }

}


/** Retry the computation until we get a `Success`.
  *
  * Be careful when using this setting since your program
  * could be blocked in case it doesn't arrive to a success.
  */
case class Forever(timeout: Long) extends RetryPolicy {

  def repeat[T](f: () => Future[T])
               (implicit ec: ExecutionContext, lg: Logger) = {
    f() recoverWith {
      case ex: Exception =>
        lg.warn(s"Retrying failed:\n\t$ex")
       repeat(f)
    }
  }

}

