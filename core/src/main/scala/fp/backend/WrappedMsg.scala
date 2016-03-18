package fp.backend

import fp.model.Message

/** Wraps a message with a context that represents some metadata of a
  * connection established with a client, e.g. the client address/port number
  * or a channel (in some backends) to send a response back to the client.
  */
trait WrappedMsg[Context] {
  val ctx: Context
  val msg: Message
}
