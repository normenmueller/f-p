/** This package, [[silt]], contains ...
  */
package object silt {

  /** A [[https://en.wikipedia.org/wiki/Host_(network) network host]] is a computer or other device connected to a
    *  computer network. A network host may offer information resources, services, and applications to users or other
    *  nodes on the network. A network host is a network node that is assigned a network layer host address.
    *
    * @param address network layer host address
    * @param port network port
    */
  final case class Host(address: String, port: Int) {
  
    override val toString = s"$address:$port"
  
  }

  /* An Id of a silo reference. */
  private[silt] case class SiloRefId(id: RefId, at: Host)

  /** A unique silo sytem message identifier. */
  final case class MsgId private[silt] (value: Int)

  /** A unique silo sytem reference identifier. */
  final case class RefId private[silt] (value: Int)

}

// vim: set tw=120 ft=scala:
