/** This package, [[silt]], contains ...
  */
package object silt {

  /** A [[https://en.wikipedia.org/wiki/Host_(network) network host]] is a
    * computer or other device connected to a computer network. A network host
    * may offer information resources, services, and applications to users or
    * other nodes on the network. It is assigned a network layer host address.
    *
    * @param address Host address
    * @param port Host port
    */
  final case class Host(address: String, port: Int) {

    override val toString = s"$address:$port"

  }

  //  /** A JVM-wide, unique identifier of a silo.
  //    *
  //    * Note, the context of a host and a VMID is necessary to unambiguously
  //    * identify silos without the requirement to request consensus in a silo
  //    * landscape spread over various nodes which, for sure, would negatively
  //    * affect performance.
  //    */
  //  final case class Id(uid: Int, at: Host, in: VMID) {
  //
  //    override val toString = s"$uid:$in @ $at"
  //
  //  }

  /** A unique silo sytem message identifier. */
  final case class MsgId private[silt] (value: Int)

  /** A unique silo sytem reference identifier. */
  final case class RefId private[silt] (value: Int)
}

