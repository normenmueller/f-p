package wc

import scala.pickling._
import scala.spores._

import fp._
import fp.SiloFactory

import fp.backend.SelfDescribing
import fp.model.{SiloSystemId, Populate, MsgId}
import fp.model.PicklingProtocol._
import sporesPicklers._

object SelfDescribingTest extends App {

  def test[T: Pickler: Unpickler](msg: T): Unit = {

    val p = msg.pickle
    val pp = p.unpickle[T]
    println("it worked!")

  }

  // Works because we don't link to any function into the spore
  val s = spore[Unit,Silo[Int]]{Unit => new Silo(1)}
  test(s)
  
  // Works because we don't link to any function into the spore
  test[Populate[Int]](Populate(MsgId(1), SiloSystemId("t"), s))
  
}

