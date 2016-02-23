package multijvm
package cs

import scala.concurrent.{ Await, ExecutionContext, Future, Promise }
import ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{ Success, Failure, Random }

import com.typesafe.scalalogging.{ StrictLogging => Logging }

import silt._

// A silo system running in client mode can be understood as ''master'', or ''driver''.
// 
// All these terms have their raison d'être, i.e., all these terms, in general, state the fact that this node is for
// defining the control flow, the to be executed computations, and for sending those computations to respective silo
// systems running in server mode.

trait Commons {

  private val text = """
In this talk, I'll present some of our ongoing work on a new programming model for asynchronous and distributed
programming. For now, we call it "function-passing" or "function-passing style", and it can be thought of as an
inversion of the actor model - keep your data stationary, send and apply your functionality (functions/spores) to that
stationary data, and get typed communication all for free, all in a friendly collections/futures-like package!
""".replace('\n', ' ').split(" ")

  private def word(random: Random): String = {
    val index = random.nextInt(words.length)
    text(index)
  }

  private val numLines = 10

  protected val words = {
    val buffer = collection.mutable.ListBuffer[String]()
    val random = new Random(100)
    for (i <- 0 until numLines) yield {
      val tenWords = for (_ <- 1 to 10) yield word(random)
      buffer += tenWords.mkString(" ")
    }
    buffer.toList
  }

}
 
object ExampleMultiJvmClient1 extends AnyRef with Commons with Logging {

  /* To be "siloed" data: Each string is a concatenation of 10 random words, separated by space
   * Alternative 1: Define data via `() => Silo[T]`
   */
  val data = () => new Silo(words)

  import logger._

  def main(args: Array[String]): Unit = try { 
    /* Start a silo system in client mode. */
    val system = Await.result(SiloSystem(), 10.seconds)
    info(s"Silo system `${system.name}` up and running.")

    /* Specify the location where to publish data. */
    val at = Host("127.0.0.1", 8090)

    /* Populate initial silo, i.e., upload initial data. */
    import scala.pickling.Defaults._
    val ref: SiloRef[List[String]] = Await.result(system.populate(at)(data), 10.seconds)    
    info(s"Data populated at ${ref.id.at}")

    /* Force execution. */
    //val result: List[String] = Await.result(ref.send(), 10.seconds)

    /* Print result. */
    //info(s"Size of list: ${result.size}")
    //info("The list:")
    //result foreach (info(_))

    Await.result(system.terminate, Duration.Inf)
  } catch { case err: Throwable => 
    logger.error(s"Silo system terminated with error `${err.getMessage}`.")
  } 
 
}

object ExampleMultiJvmClient2 extends AnyRef with Commons with Logging {

  /* To be "siloed" data: Each string is a concatenation of 10 random words, separated by space
   * Alternative 2: Define data via `SiloFactory[T]`
   */
  val data = new SiloFactory[List[String]] { def data = words }

  import logger._

  def main(args: Array[String]): Unit = try { 
    /* Start a silo system in client mode. */
    val system = Await.result(SiloSystem(), 10.seconds)
    info(s"Silo system `${system.name}` up and running.")

    /* Specify the location where to publish data. */
    val at = Host("127.0.0.1", 8090)

    /* Populate initial silo, i.e., upload initial data. */
    import scala.pickling.Defaults._
    val ref: SiloRef[List[String]] = Await.result(system.populate(at, data), 10.seconds)    
    info(s"Data populated at ${ref.id.at}")

    /* Force execution. */
    //val result: List[String] = Await.result(ref.send(), 10.seconds)

    /* Print result. */
    //info(s"Size of list: ${result.size}")
    //info("The list:")
    //result foreach (info(_))

    Await.result(system.terminate, Duration.Inf)
  } catch { case err: Throwable => 
    logger.error(s"Silo system terminated with error `${err.getMessage}`.")
  } 
 
}

// vim: set tw=120 ft=scala:
