package silt.samples
package netty
package getstarted

import scala.concurrent.{ Await }
import scala.concurrent.duration._
import scala.util.{ Try, Success, Failure }

import scala.pickling.Defaults._
import scala.pickling.shareNothing._

import com.typesafe.scalalogging.{ StrictLogging => Logging }

import silt._

object Client extends AnyRef with Logging {

  private val summary = """
In this talk, I'll present some of our ongoing work on a new programming model
for asynchronous and distributed programming. For now, we call it "function-passing"
or "function-passing style", and it can be thought of as an inversion
of the actor model - keep your data stationary, send and apply your
functionality (functions/spores) to that stationary data, and get typed
communication all for free, all in a friendly collections/futures-like
package!
"""

  private lazy val words: Array[String] =
    summary.replace('\n', ' ').split(" ")

  def word(random: scala.util.Random): String = {
    val index = random.nextInt(words.length)
    words(index)
  }

  val numLines = 10

  // each string is a concatenation of 10 random words, separated by space
  val data = () => {
    val buffer = collection.mutable.ListBuffer[String]()
    val random = new scala.util.Random(100)
    for (i <- 0 until numLines) yield {
      val tenWords = for (_ <- 1 to 10) yield word(random)
      buffer += tenWords.mkString(" ")
    }
    new Silo(buffer.toList)
  }

  def main(args: Array[String]): Unit = {
    val system = SiloSystem() match {
      case Success(system) => Await.result(system, 10.seconds)
      case Failure(error)  => sys.error(s"Could not instantiate silo system:\n ${error.getMessage}")
    }
    logger.info("Silo system in client mode up and running.")

    /* Location where to publish data */
    val target = Host("127.0.0.1", 8090)

    system.populateTo(target)(data)

    //val siloFut = system.fromFun(host)(() => populateSilo(10, new scala.util.Random(100)))
    //val done = siloFut.flatMap(_.send())

    //val res = Await.result(done, 15.seconds)
    //println("RESULT:")
    //println(s"size of list: ${res.size}")
    //res.foreach(println)

    system.shutdown()
  }

}

// vim: set tw=80 ft=scala:
