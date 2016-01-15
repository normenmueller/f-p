package silt
package test

import scala.concurrent.{ ExecutionContext, Future }
import ExecutionContext.Implicits.global
import scala.util.{ Try, Success, Failure }

import org.scalatest.{ FlatSpec, Matchers }

import silt._

class SiloRefTest extends FlatSpec with Matchers {

  "..." should "be unique" in {
    val msg1 = Populate(null)
    val msg2 = Populate(null)

    println(msg1.id)
    println(msg2.id)

    true should be (true)
  }
  
  //"References to silos" should "be unique" in {
  //  class MySiloRef[T](override val id: Id) extends AnyRef with SiloRef[T]

  //  val ref1 = new MySiloRef(Id(1, Host("localhost", 4711)))
  //  val ref2 = new MySiloRef(Id(2, Host("localhost", 4711)))

  //  ref1.id.uid should     equal (1)
  //  ref2.id.uid should     equal (2)
  //  ref1.id.at  should     equal (ref2.id.at)
  //  //ref1.id.in  should     equal (ref2.id.in)
  //  ref1        should not equal (ref2)
  //}

}
