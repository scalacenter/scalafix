package scalafix.rewrite

import scalafix.internal.config.ScalafixReporter

/** A thin wrapper around a string name and optional deprecation warning. */
final case class RewriteIdentifier(
    value: String,
    deprecated: Option[scalafix.util.Deprecated]
) {
  override def toString: String = value
}

object RewriteIdentifier {
  def apply(value: String) =
    new RewriteIdentifier(value, None)
}

/** A thin wrapper around a list of RewriteIdentifier. */
final case class RewriteName(identifiers: List[RewriteIdentifier]) {
  def name: String =
    if (identifiers.isEmpty) "empty"
    else identifiers.mkString("+")
  def isEmpty: Boolean = identifiers.isEmpty
  def +(other: RewriteName): RewriteName =
    new RewriteName((identifiers :: other.identifiers :: Nil).flatten)
  override def toString: String = name
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
  final val empty = new RewriteName(Nil)
  def apply(name: String) = new RewriteName(RewriteIdentifier(name) :: Nil)
  implicit def generate(implicit name: sourcecode.Name): RewriteName =
    RewriteName(name.value)
}
