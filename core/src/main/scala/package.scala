/** This package, [[silt]], contains ...
  */
package object silt {
  import java.util.concurrent.atomic.AtomicInteger

  private[silt] val ids = new AtomicInteger(0)

  private[silt] val JID = new java.rmi.dgc.VMID()

  /** A [[https://en.wikipedia.org/wiki/Host_(network) network host]] is a
    * computer or other device connected to a computer network. A network host
    * may offer information resources, services, and applications to users or
    * other nodes on the network. A network host is a network node that is
    * assigned a network layer host address.
    *
    * @param address network layer host address
    * @param port network port
    */
  final case class Host(address: String, port: Int) {

    override val toString = s"$address:$port"

  }

  // System messages

  private[silt] sealed abstract class Message

  case class InitSiloFromFun[T](data: () => Silo[T]) extends Message

}

// vim: set tw=80 ft=scala:
