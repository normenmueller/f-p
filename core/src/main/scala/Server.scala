package silt

trait Server extends Runnable {

  def at: Host

  def stop(): Unit

}

// vim: set tw=80 ft=scala:
