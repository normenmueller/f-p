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
  //    override val toString = s"$uid:$in @ $at"
  //  
  //  }

  /** A unique silo sytem message identifier.
    */
  final case class Id private[silt] (value: Int)

  // trait Processor {
  //   def start(): Unit

  //   def process(question: silt.Message): Unit // XXX Future[silt.Message]

  //   def stop(): Unit
  // }

}

// vim: set tw=80 ft=scala:
