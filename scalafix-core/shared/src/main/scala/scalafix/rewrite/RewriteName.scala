package scalafix.rewrite

import scala.language.implicitConversions

import scalafix.internal.config.ScalafixReporter
import scalafix.util.Deprecated

/** A thin wrapper around a list of RewriteIdentifier. */
final case class RewriteName(identifiers: List[RewriteIdentifier]) {
  private def nonDeprecated = identifiers.filter(n => n.deprecated.isEmpty)
  def withOldName(name: String, message: String, since: String): RewriteName =
    this.+(
      RewriteName(
        RewriteIdentifier(name, Some(Deprecated(message, since))) :: Nil))
  def value: String =
    if (nonDeprecated.isEmpty) "empty"
    else nonDeprecated.mkString("+")
  def isEmpty: Boolean = identifiers.isEmpty
  def +(other: RewriteName): RewriteName =
    new RewriteName((identifiers :: other.identifiers :: Nil).flatten)
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

object RewriteName {
  implicit def stringToRewriteName(name: String): RewriteName =
    RewriteName(name)
  final val empty = new RewriteName(Nil)
  def apply(name: String) = new RewriteName(RewriteIdentifier(name) :: Nil)
}
