package silt
package test

import scala.concurrent.{ ExecutionContext, Future }
import ExecutionContext.Implicits.global
import scala.util.{ Try, Success, Failure }

import org.scalatest.{ FlatSpec, Matchers }

import silt._

class SiloRefTest extends FlatSpec with Matchers {

  "References to silos" should "be unique" in {
    class MySiloRef[T](override val at: Host) extends AnyRef with SiloRef[T]

    val ref1 = new MySiloRef(Host("localhost", 4711))
    val ref2 = new MySiloRef(Host("localhost", 4711))

    ref1.id.uid should     equal (1)
    ref2.id.uid should     equal (2)
    ref1.id.at  should     equal (ref2.id.at)
    ref1.id.in  should     equal (ref2.id.in)
    ref1        should not equal (ref2)
  }

}
