package scalafix.rule

import scala.language.implicitConversions

import scalafix.internal.config.ScalafixReporter
import scalafix.util.Deprecated

/** A thin wrapper around a list of RuleIdentifier. */
final case class RuleName(identifiers: List[RuleIdentifier]) {
  private def nonDeprecated = identifiers.filter(n => n.deprecated.isEmpty)

  def withDeprecatedName(
      name: String,
      message: String,
      since: String): RuleName = {
    val conflictingName = identifiers.find(_.value == name)
    require(conflictingName.isEmpty, s"Duplicate name! $conflictingName")
    this.+(
      RuleName(RuleIdentifier(name, Some(Deprecated(message, since))) :: Nil))
  }
  @deprecated("Renamed to withDeprecatedName", "0.5.0")
  def withOldName(name: String, message: String, since: String): RuleName =
    withDeprecatedName(name, message, since)
  def value: String =
    if (nonDeprecated.isEmpty) "empty"
    else nonDeprecated.mkString("+")
  def isEmpty: Boolean = identifiers.isEmpty
  def +(other: RuleName): RuleName =
    new RuleName((identifiers :: other.identifiers :: Nil).flatten)
  override def toString: String = value
  def reportDeprecationWarning(name: String, reporter: ScalafixReporter): Unit = {
    identifiers.foreach { ident =>
      if (ident.value == name) {
        ident.deprecated.foreach { d =>
          reporter.warn(
            s"Name $name is deprecated. ${d.message} (since ${d.since})")
        }
      }
    }
  }
}

object RuleName {
  implicit def stringToRuleName(name: String): RuleName =
    RuleName(name)

  @deprecated(
    "Rule names will soon no longer be automatically generated with macros. " +
      "Write the name explicitly as a string instead.",
    "0.5.0")
  implicit def generate(name: sourcecode.Name): RuleName = RuleName(name.value)
  final val empty = new RuleName(Nil)
  def apply(name: String) = new RuleName(RuleIdentifier(name) :: Nil)
}
