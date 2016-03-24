package fp
package test

import fp.backend.netty.SiloSystem

import scala.concurrent.{ Await, ExecutionContext, Future, Promise }
import scala.concurrent.duration._
import ExecutionContext.Implicits.global
import scala.util.{ Try, Success, Failure }

import org.scalatest.{ FlatSpec, Matchers }

class SiloSystemTest extends FlatSpec with Matchers {

  def initSilo = Await.ready(SiloSystem(), 10.seconds)

  "Instantiation" should "yield a default silo system" in {
    initSilo map { _ shouldBe a[SiloSystem] }
  } 

  it should "use the default, Netty-based realization" in {
    initSilo map { _ shouldBe a[SiloSystem] }
  } 

  it should "throw an exception in case of wrong `silo.system.impl` parameter" in {
    val osp = Option(System.getProperty("silo.system.impl"))

    System.setProperty("silo.system.impl", "XXX")
    initSilo map { _ shouldBe a[ClassNotFoundException] }

    osp match {
      case None    => System.clearProperty("silo.system.impl")
      case Some(v) => System.setProperty("silo.system.impl", v)
    }
  }

  it should "throw an exception in case of port is already taken" in {
    val system = Await.result(SiloSystem(Some(8090)), 10.seconds)
    Try(Await.result(SiloSystem(Some(8090)), 10.seconds)) match {
      case Success(s) =>
        Await.result(s.terminate(), 10.seconds)
        true shouldBe false 
      case Failure(e) => e shouldBe a[java.net.BindException]
    }
    Await.result(system.terminate(), 10.seconds)
  }

}

