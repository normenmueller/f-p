package silt
package core

import scala.pickling.{Pickler, Unpickler}

sealed abstract class Command

case class DoPumpTo[A, B, P](
  node: Node, fun: P, emitterId: Int, destHost: Host, destRefId: Int,
  pickler: Pickler[P], unpickler: Unpickler[P]
) extends Command with Serializable[P]

case class CommandEnvelope(cmd: Command)
