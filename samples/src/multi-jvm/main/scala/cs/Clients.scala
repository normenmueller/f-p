package multijvm
package cs

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import scala.pickling._
import scala.spores._

import fp._
import fp.model.PicklingProtocol._
import sporesPicklers._

import fp.backend.netty.SiloSystem

import com.typesafe.scalalogging.{StrictLogging => Logging}

/** A silo system running in client mode can be understood as ''master'', or ''driver''.
  *
  * All these terms have their raison d'être, i.e., all these terms, in general,
  * state the fact that this node is for defining the control flow, the to be
  * executed computations, and for sending those computations to respective silo
  * systems running in server mode.
  */
object Clients extends App with Logging {

  private val summary = """
In this talk, I'll present some of our ongoing work on a new programming model for asynchronous and distributed
programming. For now, we call it "function-passing" or "function-passing style", and it can be thought of as an
inversion of the actor model - keep your data stationary, send and apply your functionality (functions/spores) to that
stationary data, and get typed communication all for free, all in a friendly collections/futures-like package!
                        """

  private lazy val words: Array[String] = summary.replace('\n', ' ').split(" ")

  def word(random: scala.util.Random): String = {
    val index = random.nextInt(words.length)
    words(index)
  }

  val numLines = 10

  // Alternative 1: Define data via `() => Silo[T]`
  /*
  val data = () => {
    val buffer = collection.mutable.ListBuffer[String]()
    val random = new scala.util.Random(100)
    for (i <- 0 until numLines) yield {
      val tenWords = for (_ <- 1 to 10) yield word(random)
      buffer += tenWords.mkString(" ")
    }
    new Silo(buffer.toList)
  }
  */

  //List(1,2).pickle
  //implicit val e = implicitly[Pickler[List[String]]]

  // Alternative 2: Define data via `SiloFactory[T]`
  /* ON HOLD until bugs of scala pickling are fixed
   *val data = SiloFactory { () => {
    val buffer = collection.mutable.ListBuffer[String]()
    val random = new scala.util.Random(100)
    for (i <- 0 until numLines) yield {
      val tenWords = for (_ <- 1 to 10) yield word(random)
      buffer += tenWords.mkString(" ")
    }
    buffer.toList
  }}*/

  // Alternative 3
  /* NOTE: Spores have to be defined here statically */
  val s = spore[Unit, Silo[List[String]]] {
    Unit => new Silo(List("1", "2", "3"))
  }

  //val s2 = s.pickle.unpickle[Spore[Unit, Silo[List[String]]]]
  //val mt = s2(())

  val fs: Spore[List[String], List[Int]] = (l: List[String]) => l.map(_.toInt)
  implicitly[Pickler[List[String]]]
  implicitly[Unpickler[List[String]]]
  implicitly[Pickler[List[Int]]]
  implicitly[Unpickler[List[Int]]]
  implicitly[Pickler[Spore[List[String], List[Int]]]]
  implicitly[Unpickler[Spore[List[String], List[Int]]]]
  val data = new SiloFactory(s)

  import logger._

  try {
    /* Start a silo system in client mode. */
    implicit val system = Await.result(SiloSystem(), 10.seconds)
    info(s"Silo system `${system.name}` up and running.")

    /* Specify the location where to publish data. */
    val host = Host("127.0.0.1", 8999)

    /* Populate initial silo, i.e., upload initial data. */
    val ref: SiloRef[List[String]] =
      Await.result(data.populateAt(host), 25.seconds)
    info(s"Data populated at ${ref.id.at}")

    val mapped = Await.result(ref.map(fs).send, 25.seconds)

    /* Force execution. */
    //val result: List[String] = Await.result(ref.send, 10.seconds)

    /* Print result. */
    //info(s"Size of list: ${result.size}")
    //info("The list:")
    //result foreach (info(_))

    Await.result(system.terminate, Duration.Inf)
  } catch { case err: Throwable =>
    logger.error(s"Silo system terminated with error:\n $err.")
  }

}

