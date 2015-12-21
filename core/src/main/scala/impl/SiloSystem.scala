package silt
package impl

import scala.concurrent.Future

trait SiloSystem extends silt.SiloSystem with SiloSystemInternal {

  def withServer(at: Option[Host]): Future[SiloSystem]

}

// vim: set tw=80 ft=scala:
