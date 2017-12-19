package scalafix.internal.rule

import scala.meta._
import scala.meta.tokens.Token._
import scalafix.Patch
import scalafix.rule.{Rule, RuleCtx}

case object DottyAutoTuplingFunctionArgs
    extends Rule("DottyAutoTuplingFunctionArgs") {
  override def description: String =
    "Rewrite that removes pattern-matching decomposition if function arguments can be automatically tupled in Dotty."

  override def fix(ctx: RuleCtx): Patch =
    ctx.tree.collect {
      case Term.Apply((_, List(pf: Term.PartialFunction)))
          if canBeAutoTupled(pf) =>
        val pfTokens = pf.tokens

        (for {
          leftBrace <- pfTokens.headOption if leftBrace.is[LeftBrace]
          rightBrace <- pfTokens.lastOption if rightBrace.is[RightBrace]
          kwCase <- pfTokens.find(_.is[KwCase])
        } yield {
          val tkList = ctx.tokenList
          val replacedBraces =
            if (canReplaceBracesWithParens(pf)) {
              ctx.replaceToken(leftBrace, "(") +
                ctx.replaceToken(rightBrace, ")") +
                ctx.removeTokens(tkList.leadingSpaces(leftBrace)) +
                ctx.removeTokens(tkList.trailingSpaces(leftBrace)) +
                ctx.removeTokens(tkList.leadingSpaces(rightBrace))
            } else Patch.empty

          replacedBraces +
            ctx.removeToken(kwCase) +
            ctx.removeTokens(tkList.trailingSpaces(kwCase))
        }).asPatch
    }.asPatch

  private def canBeAutoTupled(pf: Term.PartialFunction): Boolean =
    pf.cases match {
      case Case(Pat.Tuple(args), None, _) :: Nil
          if args.forall(a => a.is[Pat.Var] || a.is[Pat.Wildcard]) =>
        true
      case _ => false
    }

  private def canReplaceBracesWithParens(pf: Term.PartialFunction): Boolean =
    pf.pos.startLine == pf.pos.endLine && pf.cases.head.stats.size == 1
}
