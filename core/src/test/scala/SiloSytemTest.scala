package silt
package test

import scala.concurrent.{ Await, ExecutionContext, Future, Promise }
import scala.concurrent.duration._
import ExecutionContext.Implicits.global
import scala.util.{ Try, Success, Failure }

import org.scalatest.{ FlatSpec, Matchers }

class SiloSystemTest extends FlatSpec with Matchers {

  "Instantiation" should "yield a default silo system" in {
    Await.ready(silt.SiloSystem(), 10.seconds) map { _ shouldBe a [silt.SiloSystem] }
  } 

  it should "use the default, Netty-based realization" in {
    Await.ready(silt.SiloSystem(), 10.seconds) map { _ shouldBe a [silt.impl.SiloSystem] }
  } 

  it should "throw an exception in case of wrong `silo.system.impl` parameter" in {
    val osp = Option(System.getProperty("silo.system.impl"))

    System.setProperty("silo.system.impl", "XXX")
    Await.ready(silt.SiloSystem(), 10.seconds) map { _ shouldBe a [ClassNotFoundException] } 

    osp match {
      case None    => System.clearProperty("silo.system.impl")
      case Some(v) => System.setProperty("silo.system.impl", v)
    }
  }

  it should "throw an exception in case of port is already taken" in {
    val system = Await.result(silt.SiloSystem(Some(8090)), 10.seconds)
    Try(Await.result(silt.SiloSystem(Some(8090)), 10.seconds)) match {
      case Success(s) =>
        Await.result(s.terminate(), 10.seconds)
        true shouldBe false 
      case Failure(e) => e shouldBe a [java.net.BindException]
    }
    Await.result(system.terminate(), 10.seconds)
  }

}

// vim: set tw=80 ft=scala:
