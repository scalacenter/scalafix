package scalafix.v1

/** An implicit instance of SemanticContext implies the call-site has access to semantic APIs.
  *
  * Useful to guard users from calling semantic operations without access to semantic APIs.
  */
trait SemanticContext
