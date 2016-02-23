package silt

sealed abstract class Command

case class DoPumpTo[A, B, P](
  node: Node, fun: P, emitterId: Int, destHost: Host, destRefId: Int
) extends Command with Serializable[P]

case class CommandEnvelope(cmd: Command)
