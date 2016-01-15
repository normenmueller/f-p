package silt
package test

import scala.concurrent.{ ExecutionContext, Future }
import ExecutionContext.Implicits.global
import scala.util.{ Try, Success, Failure }

import org.scalatest.{ FlatSpec, Matchers }

class SiloSystemTest extends FlatSpec with Matchers {

  "Instantiation" should "yield a default silo system" in {
    silt.SiloSystem() map { _ map { _ shouldBe a [silt.SiloSystem] } }
  }

  it should "fulfill the implementation requirements of a silo system" in {
    silt.SiloSystem() map { _ map { _ shouldBe a [silt.impl.Requirements] } }
  }

  it should "use default, Netty-based realization" in {
    silt.SiloSystem() map { _ map { _ shouldBe a [silt.impl.SiloSystem] } }
  }

  it should "throw an execption in case of wrong `silo.system.impl` parameter" in {
    val osp = Option(System.getProperty("silo.system.impl"))

    System.setProperty("silo.system.impl", "XXX")
    silt.SiloSystem().failed map { _ shouldBe a [ClassNotFoundException] } 

    osp match {
      case None    => System.clearProperty("silo.system.impl")
      case Some(v) => System.setProperty("silo.system.impl", v)
    }
  }

}
