package fp
package backend

/** The internal API of a F-P server. */
private[backend] trait Server extends Runnable {

  /** Host this server is running at */
  def host: Host

  /** Start server */
  def start(): Unit

  /** Stop server */
  def stop(): Unit

  /** Once the thread is spawned, start the server */
  final override def run(): Unit = {
    start()
  }

}

