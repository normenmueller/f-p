package wc

import scala.pickling._
import scala.spores._

import fp._
import fp.SiloFactory

import fp.backend.SelfDescribing
import fp.model.Populate
import fp.model.MsgId
import fp.model.PicklingProtocol._
import sporesPicklers._

object SelfDescribingTest extends App {

  def test[T: Pickler: Unpickler](msg: T): Unit = {

    // 1. generate unpickler
    val unpickler = implicitly[Unpickler[T]]
    
    // 2. pickle value
    val p = msg.pickle

    val pp = p.unpickle[T]
    println("it worked!")

  }

  // Works because we don't link to any function into the spore
  val s = spore[Unit,Silo[Int]]{Unit => new Silo(1)}
  test(s)
  
  // Doesn't work because we have a reference to a function inside the
  // spore and the unpickler is badly generated somehow...
  val n: Spore[Unit, Int] = Unit => 1
  val s2 = spore[Unit,Int]{
    val g = n
    Unit => g()
  }
  test(s2)

  // Works because we don't link to any function into the spore
  //test(Populate(MsgId(1), spore[Unit,Silo[Int]]{Unit => new Silo(1)}))
  

  // Doesn't work because we have a reference to a function inside the
  // spore and the unpickler is badly generated somehow...
  // val sf = SiloFactory(() => 1)
  // test(Populate(MsgId(1), sf.s))

}

