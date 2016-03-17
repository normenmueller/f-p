package fp.backend.netty

import java.util.concurrent.{BlockingQueue, CountDownLatch}

import com.typesafe.scalalogging.{StrictLogging => Logging}
import fp.backend.netty.handlers.PopulateHandler
import fp.model.{Populate}
import fp.util.AsyncExecution

import scala.concurrent.ExecutionContext

private[netty] class Receptor(mq: BlockingQueue[NettyWrapper])
                             (implicit val ec: ExecutionContext, server: Server)
  extends Runnable with AsyncExecution with Logging {

  import logger._

  private val counter = new CountDownLatch(1)

  def start(): Unit = {
    trace("Receptor started.")

    while (counter.getCount > 0) processMsg(mq.take())
  }

  def processMsg(wrapper: NettyWrapper): Unit = {
    wrapper match {
      case NettyWrapper(ctx, msg) => msg match {
        case m: Populate[_] => PopulateHandler.process(m, ctx)
        case _ => error(s"We weren't able to handle $msg")
      }

      case x => error(s"We received a non Netty-wrapped message:\n$x")
    }
  }

  def stop(): Unit = {
    trace("Receptor stop...")

    counter.countDown()

    trace("Receptor stop done.")
  }

  final override def run(): Unit = start()

}

