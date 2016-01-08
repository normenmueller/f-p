package silt.samples
package netty
package getstarted

object Server extends AnyRef with App {

  silt.SiloSystem(Some(8090))

}

// vim: set tw=80 ft=scala:
