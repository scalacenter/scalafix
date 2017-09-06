package scalafix.internal.rule

import scala.collection.immutable.Seq
import scala.meta._
import scala.meta.tokens.Token.LeftBrace
import scala.meta.tokens.Token.RightParen
import scalafix.Patch
import scalafix.rule.Rule
import scalafix.rule.RewriteCtx

case object ProcedureSyntax extends Rule {
  def name = "ProcedureSyntax"
  override def fix(ctx: RewriteCtx): Patch = {
    val patches: Seq[Patch] = ctx.tree.collect {
      case t: Defn.Def
          if t.decltpe.exists(_.tokens.isEmpty) &&
            t.body.tokens.head.is[LeftBrace] =>
        val bodyStart = t.body.tokens.head
        val defEnd = (for {
          lastParmList <- t.paramss.lastOption
          lastParam <- lastParmList.lastOption
        } yield lastParam.tokens.last).getOrElse(t.name.tokens.last)
        val closingParen =
          ctx.tokenList
            .slice(defEnd, bodyStart)
            .find(_.is[RightParen])
            .getOrElse(defEnd)
        ctx.addRight(closingParen, s": Unit =")
    }
    patches.asPatch
  }
}
