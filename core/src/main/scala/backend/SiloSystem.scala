package silt
package impl

import scala.concurrent.Future

trait SiloSystem extends silt.SiloSystem with silt.Internals {

  /** Return an realization agnostic silo system running in server mode.
    *
    * A silo system running in server mode has an underlying [[silt.impl.Server]]
    * to host silos and make those available to other silo systems.
    *
    * The underlying server is private to the silo system, i.e., only the silo
    * system itself directly communicates with the server. A user/client only
    * directly communicates with a silo system as such.
    *
    * @param at Target host
    */
  def withServer(at: Host): Future[silt.SiloSystem]

}

