package scalafix.rule

import scalafix.util.Deprecated

/** A thin wrapper around a string name and optional deprecation warning. */
final case class RuleIdentifier(
    value: String,
    deprecated: Option[Deprecated]
) {
  override def toString: String = value
}

object RuleIdentifier {
  def apply(value: String): RuleIdentifier =
    new RuleIdentifier(value, None)
}
