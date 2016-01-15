/** This package, [[silt]], contains ...
  */
package object silt {

  //  /** A JVM-wide, unique identifier of a silo.
  //    *
  //    * Note, the context of a host and a VMID is necessary to unambiguously
  //    * identify silos without the requirement to request consensus in a silo
  //    * landscape spread over various nodes which, for sure, would negatively
  //    * affect performance.
  //    */
  //  final case class Id(uid: Int, at: Host, in: VMID) {
  //  
  //    //override val toString = s"$uid:$in @ $at"
  //    override val toString = s"$uid @ $at"
  //  
  //  }

  final case class Id private[silt] (value: Int)

  final case class Terminated(message: String)

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

  trait Server extends Runnable {

    def at: Host

    def stop(): Unit

  }

  trait Hostable {

    self: silt.SiloSystem =>

    /** Return an implementation agnostic silo system running in server mode.
      *
      * @param at [[Host network host]]
      */
    def withServer(at: Host): scala.concurrent.Future[silt.SiloSystem with Server]

  }

  // trait Processor {
  //   def start(): Unit

  //   def process(question: silt.Message): Unit // XXX Future[silt.Message]

  //   def stop(): Unit
  // }

}

// vim: set tw=80 ft=scala:
