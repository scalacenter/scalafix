package scalafix.internal.rule

import scalafix.v1._
import scala.meta._
import metaconfig.Configured
import scalafix.util.TokenList

class RedundantTokens(config: RedundantTokensConfig)
    extends SemanticRule("RedundantTokens") {
  def this() = this(RedundantTokensConfig())
  override def withConfiguration(config: Configuration): Configured[Rule] =
    config.conf
      .getOrElse("redundantTokens", "RedundantTokens")(
        RedundantTokensConfig.default
      )
      .map(new RedundantTokens(_))

  override def description: String =
    "Removes redundant tokens such as `final` modifiers on an object"
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
