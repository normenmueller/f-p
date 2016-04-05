package fp.model

/** Keeps all the message errors that are needed to give feedback
  * to the user in case of a failure. It's kept separate to allow
  * easy modification and not to pollute the logic of f-p. */
object Feedback {

  val receptionAlgorithmFailed = s"""
    |A message id less than `expectedMsgId.value - 1` has been received.
    |This means that the invariants of the protocol have been violated
    |and there's a bug either in the client or in the server.
  """.stripMargin

}
