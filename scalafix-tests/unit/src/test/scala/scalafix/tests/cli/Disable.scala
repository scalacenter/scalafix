package scalafix.tests.cli

import metaconfig.Configured
import scalafix.internal.util.SymbolOps
import scalafix.v1._
import scala.meta._
import scalafix.internal.util.PositionSyntax._

//
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
