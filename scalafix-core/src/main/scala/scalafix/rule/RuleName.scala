package scalafix.rule

import scala.language.implicitConversions
import scalafix.internal.config.ScalafixReporter
import scalafix.util.Deprecated

/** A thin wrapper around a list of RuleIdentifier. */
final case class RuleName(identifiers: List[RuleIdentifier]) {
  private def nonDeprecated = identifiers.filter(n => n.deprecated.isEmpty)

  def isDeprecated: Boolean = identifiers.forall(_.deprecated.isDefined)

  def matches(name: String): Boolean =
    identifiers.exists(_.value == name)

  def withDeprecatedName(
      name: String,
      message: String,
      since: String): RuleName = {
    val conflictingName = identifiers.find(_.value == name)
    require(conflictingName.isEmpty, s"Duplicate name! $conflictingName")
    this.+(
      RuleName(RuleIdentifier(name, Some(Deprecated(message, since))) :: Nil))
  }
  def value: String =
    if (isDeprecated) identifiers.mkString("+")
    else {
      nonDeprecated match {
        case Nil => "empty"
        case head :: _ => head.value
      }
    }
  def isEmpty: Boolean = identifiers.isEmpty

  def +(other: RuleName): RuleName =
    new RuleName(identifiers ::: other.identifiers)

  override def toString: String = value
  def reportDeprecationWarning(
      name: String,
      reporter: ScalafixReporter): Unit = {
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

  def deprecated(name: String, message: String, since: String): RuleName =
    new RuleName(RuleIdentifier(name, Some(Deprecated(message, since))) :: Nil)
  implicit def stringToRuleName(name: String): RuleName =
    RuleName(name)

  final val empty: RuleName = new RuleName(Nil)
  def apply(name: String): RuleName = new RuleName(RuleIdentifier(name) :: Nil)
}
