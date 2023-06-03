package scalafix.internal.rule

import scala.meta.Importee
import scala.meta.Name
import scala.meta.Term
import scala.meta.XtensionSyntax

import scalafix.lint.Diagnostic
import scalafix.lint.LintSeverity

case class ImporterSymbolNotFound(ref: Term.Name) extends Diagnostic {
  override def position: meta.Position = ref.pos

  override def message: String =
    s"Could not determine whether '${ref.syntax}' is fully-qualified because the symbol" +
      " information is missing. We will continue processing assuming that it is fully-qualified." +
      " Please check whether the corresponding .semanticdb file is properly generated."

  override def severity: LintSeverity = LintSeverity.Warning
}

case class TooManyAliases(name: Name, renames: Seq[Importee.Rename])
    extends Diagnostic {
  assert(renames.length > 1)

  override def position: meta.Position = name.pos

  override def message: String = {
    val aliases = renames
      .map { case Importee.Rename(_, Name(alias)) => alias }
      .sorted
      .map("'" + _ + "'")

    val aliasList = aliases match {
      case head :: last :: Nil => s"$head and $last"
      case init :+ last => init.mkString("", ", ", s", and $last")
    }

    s"When `OrganizeImports.groupedImports` is set to `Merge`, renaming a name to multiple" +
      s" aliases within the same source file is not supported. In this case, '${name.value}' is" +
      s" renamed to $aliasList."
  }

  override def severity: LintSeverity = LintSeverity.Error
}
