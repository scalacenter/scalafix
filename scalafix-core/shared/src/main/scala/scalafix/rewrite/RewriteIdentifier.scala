package scalafix.rewrite

import scalafix.util.Deprecated

/** A thin wrapper around a string name and optional deprecation warning. */
final case class RewriteIdentifier(
    value: String,
    deprecated: Option[Deprecated]
) {
  override def toString: String = value
}

object RewriteIdentifier {
  def apply(value: String) =
    new RewriteIdentifier(value, None)
}
