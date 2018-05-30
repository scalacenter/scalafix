package scalafix.internal.rule

import scala.meta._
import scalafix.v0._
import scalafix.util.Trivia

case object ProcedureSyntax
    extends Rule(
      RuleName("ProcedureSyntax").withDeprecatedName(
        "ExplicitUnit",
        "Use ProcedureSyntax instead",
        "0.6.0"
      )
    ) {

  override def description: String =
    "Rewrite that inserts explicit : Unit = for soon-to-be-deprecated procedure syntax def foo { ... }"

  override def fix(ctx: RuleCtx): Patch = {
    ctx.tree.collect {
      case t: Decl.Def if t.decltpe.tokens.isEmpty =>
        ctx.addRight(t.tokens.last, s": Unit").atomic
      case t: Defn.Def
          if t.decltpe.exists(_.tokens.isEmpty) &&
            t.body.tokens.head.is[Token.LeftBrace] =>
        val fixed = for {
          bodyStart <- t.body.tokens.headOption
          toAdd <- ctx.tokenList.leading(bodyStart).find(!_.is[Trivia])
        } yield ctx.addRight(toAdd, s": Unit =").atomic
        fixed.getOrElse(Patch.empty)
    }.asPatch
  }
}
