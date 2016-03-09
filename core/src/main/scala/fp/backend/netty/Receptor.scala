package fp
package backend
package netty

import java.util.concurrent.{BlockingQueue, CountDownLatch}

import com.typesafe.scalalogging.{StrictLogging => Logging}
import fp.model.{Populate, Populated}
import fp.util.AsyncExecution

import scala.pickling.Defaults._

private[netty] class Receptor(mq: BlockingQueue[NettyWrapper])
    extends Runnable with Helper with AsyncExecution with Logging {

  import logger._

  override implicit val ec = scala.concurrent.ExecutionContext.global
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

