package scalafix.internal.rule

import scala.meta._

import metaconfig.Configured
import scalafix.v1._

class RedundantSyntax(config: RedundantSyntaxConfig)
    extends SyntacticRule("RedundantSyntax") {
  def this() = this(RedundantSyntaxConfig())
  override def withConfiguration(config: Configuration): Configured[Rule] =
    config.conf
      .getOrElse("redundantSyntax", "RedundantSyntax")(
        RedundantSyntaxConfig.default
      )
      .map(new RedundantSyntax(_))

  override def description: String =
    "Removes redundant syntax such as `final` modifiers on an object"
  override def isRewrite: Boolean = true

  override def fix(implicit doc: SyntacticDocument): Patch =
    doc.tree
      .collect {
        case o: Defn.Object if config.finalObject =>
          o.mods.find(_.is[Mod.Final]).fold(Patch.empty) { modf =>
            val tokens = modf.tokens
            val end = tokens.skipWideIf(_.is[Token.Whitespace], tokens.length)
            Patch.removeTokens(tokens.sliceWide(0, end))
          }
        case Term.Interpolate(
              interpolator,
              lit :: Nil,
              Nil
            )
            if config.stringInterpolator &&
              !mustKeepInterpolator(interpolator, lit) =>
          Patch.removeTokens(interpolator.tokens)
      }
      .map(_.atomic)
      .asPatch

  private def mustKeepInterpolator(interpolator: Tree, lit: Tree) = {
    val escapedCharacter = lit.syntax.contains('\\')
    // termInterpolate.syntax.contains("\"\"\"") does not work in scala 2.13 and scala 3
    // as the syntax uses single quotes even if the litteral was defined with triple quotes
    val tripleQuotes = lit.pos.start - interpolator.pos.end == 3
    interpolator.syntax match {
      case "s" | "f" =>
        escapedCharacter && tripleQuotes
      case "raw" =>
        escapedCharacter && !tripleQuotes
      case _ => true
    }
  }
}
