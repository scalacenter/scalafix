package scalafix.util

/** Identical to scala.deprecated except it's a case class. */
final case class Deprecated(message: String, since: String)
