package fp.backend.netty

import java.util.concurrent.{BlockingQueue, CountDownLatch}

import com.typesafe.scalalogging.{StrictLogging => Logging}
import fp.RefId
import fp.model.{Populate, Populated}
import fp.util.AsyncExecution

import scala.concurrent.ExecutionContext
import scala.pickling.Defaults._

private[netty] class Receptor(mq: BlockingQueue[NettyWrapper])
                             (implicit val ec: ExecutionContext)
  extends Runnable with Tell with AsyncExecution with Logging {

  import logger._

  private val counter = new CountDownLatch(1)

  def start(): Unit = {
    trace("Receptor started.")

    while (counter.getCount > 0) processMsg(mq.take())
  }

  def processMsg(wrapper: NettyWrapper): Unit = {
    wrapper match {
      case NettyWrapper(ctx, msg) => msg match {
        case Populate(msgId, fun) =>
          val refId = RefId(0)
          tell(ctx.channel(), Populated(msgId, refId))
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

