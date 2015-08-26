package silt
package impl

import scala.util.Try

trait SiloSystem extends silt.SiloSystem with SiloSystemInternal {

  def withServer(at: Option[Host]): Try[SiloSystem]

}

// vim: set tw=80 ft=scala:
