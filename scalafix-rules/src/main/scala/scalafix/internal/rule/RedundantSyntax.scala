package scalafix.internal.rule

import scala.meta._

import metaconfig.Configured
import scalafix.util.TokenList
import scalafix.v1._

class RedundantSyntax(config: RedundantSyntaxConfig)
    extends SemanticRule("RedundantSyntax") {
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

  override def fix(implicit doc: SemanticDocument): Patch =
    doc.tree.collect {
      case o: Defn.Object
          if config.finalObject && o.mods.exists(_.is[Mod.Final]) =>
        Patch.removeTokens {
          o.tokens.find(_.is[Token.KwFinal]).toIterable.flatMap { finalTok =>
            finalTok :: TokenList(o.tokens).trailingSpaces(finalTok).toList
          }
        }
    }.asPatch
}
