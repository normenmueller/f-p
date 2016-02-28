package fp
package backend
package netty

import java.util.concurrent.{ BlockingQueue, CountDownLatch }

import fp.model.{ Populate, Populated }

import scala.pickling._
import Defaults._

import com.typesafe.scalalogging.{ StrictLogging => Logging }

private[netty] class Receptor(mq: BlockingQueue[NettyWrapper])
    extends AnyRef with Tell with Runnable with Logging {

  import logger._

  private val latch = new CountDownLatch(1)

  def start(): Unit = {
    trace("Receptor started.")

    while (latch.getCount > 0) mq.take() match {
      case NettyWrapper(ctx, Populate(msgId, fun)) =>
        val refId = RefId(0) // XXX create silo
        tell(ctx.channel(), Populated(msgId, refId))
      case msg =>
        info(s"Skipping: $msg")
    }
  }

  def stop(): Unit = {
    trace("Receptor stop...")

    latch.countDown()

    trace("Receptor stop done.")
  }

  final override def run(): Unit = start()

}

