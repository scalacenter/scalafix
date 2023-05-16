package scalafix.test.cli

import scala.meta._

import metaconfig.Configured
import scalafix.internal.util.PositionSyntax._
import scalafix.internal.util.SymbolOps
import scalafix.v0.LintCategory
import scalafix.v1
import scalafix.v1._

class CrashingRule extends SyntacticRule("CrashingRule") {
  override def fix(implicit doc: SyntacticDocument): _root_.scalafix.v1.Patch =
    throw new MissingSymbolException(Symbol("local63"))
}

class NoOpRule extends SyntacticRule("NoOpRule") {
  override def fix(implicit doc: SyntacticDocument): _root_.scalafix.v1.Patch =
    Patch.empty
}

class DeprecatedName
    extends SyntacticRule(
      RuleName("DeprecatedName").withDeprecatedName(
        "OldDeprecatedName",
        "Use DeprecatedName instead",
        "1.0"
      )
    ) {
  override def fix(implicit doc: SyntacticDocument): _root_.scalafix.v1.Patch =
    Patch.empty
}

class Scala2_9 extends SyntacticRule("Scala2_9") {
  override def withConfiguration(config: Configuration): Configured[Rule] =
    if (!config.scalaVersion.startsWith("2.9")) {
      Configured.error("scalaVersion must start with 2.9")
    } else if (!config.scalacOptions.contains("-Ysource:2.9")) {
      Configured.error("scalacOptions must contain -Ysource:2.9")
    } else {
      Configured.ok(this)
    }
}

class AvailableRule
    extends v1.SemanticRule(
      v1.RuleName("AvailableRule")
        .withDeprecatedName("DeprecatedAvailableRule", "", "")
    )

class Disable(symbols: List[Symbol]) extends SemanticRule("Disable") {
  def this() = this(Nil)
  override def withConfiguration(config: Configuration): Configured[Rule] =
    Configured.ok(
      new Disable(
        config.conf.dynamic.Disable.symbols.as[List[Symbol]].getOrElse(Nil)
      )
    )
  override def fix(implicit doc: SemanticDocument): Patch = {
    doc.internal.textDocument.occurrences.collect {
      case o if symbols.contains(SymbolOps.normalize(Symbol(o.symbol))) =>
        val r = o.range.get
        val pos = Position.Range(
          doc.input,
          r.startLine,
          r.startCharacter,
          r.endLine,
          r.endCharacter
        )
        if (pos.lineContent.contains("import")) {
          Patch.empty
        } else {
          Patch.lint(Diagnostic(Symbol(o.symbol).displayName, "disabled", pos))
        }
    }.asPatch
  }
}

class LintError extends SyntacticRule("LintError") {
  override def fix(implicit doc: SyntacticDocument): Patch = {
    val failure = LintCategory.error("failure", "Error!")
    Patch.lint(failure.at(doc.tree.pos))
  }
}

class LintWarning extends SyntacticRule("LintWarning") {
  override def fix(implicit doc: SyntacticDocument): Patch = {
    val failure = LintCategory.warning("warning", "Warning!")
    Patch.lint(failure.at(doc.tree.pos))
  }
}
