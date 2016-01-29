package silt

trait Server {

  def start(): Unit

  def at: Host

  def stop(): Unit

}

// vim: set tw=80 ft=scala:
