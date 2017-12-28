package scalafix.internal.rule

import metaconfig.{Conf, Configured}

import scala.meta._
import scalafix.{Patch, Rule}
import scalafix.internal.config.FixSyntaxConfig
import scalafix.rule.RuleCtx

final case class FixSyntax(config: FixSyntaxConfig = FixSyntaxConfig())
    extends Rule("FixSyntax") {

  override def description: String =
    "Rule that rewrites configurable set of keywords and syntax."

  override def init(config: Conf): Configured[Rule] =
    config
      .getOrElse("fixSyntax", "FixSyntax")(FixSyntaxConfig.default)
      .map(FixSyntax(_))

  override def fix(ctx: RuleCtx): Patch = {
    ctx.tree.collect {
      case t @ Defn.Class(mods, _, _, _, _)
          if config.addFinalCaseClass &&
            mods.exists(_.is[Mod.Case]) &&
            !mods.exists(_.is[Mod.Final]) =>
        ctx.addLeft(t, "final ")
      case t @ Defn.Val(mods, _, _, _)
          if config.removeFinalVal &&
            mods.exists(_.is[Mod.Final]) =>
        val finalTokens = mods.find(_.is[Mod.Final]).get.tokens
        ctx.removeTokens(
          t.tokens.filter(
            token =>
              token.start >= finalTokens(0).start &&
                token.end <= finalTokens.last.end + 1)
        ) // remove one space after final
    }.asPatch
  }
}
