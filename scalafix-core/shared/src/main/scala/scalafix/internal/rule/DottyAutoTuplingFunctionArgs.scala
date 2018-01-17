package scalafix.internal.rule

import scala.meta._
import scala.meta.tokens.Token._
import scalafix._
import scalafix.syntax._
import scalafix.util.SymbolMatcher

case class DottyAutoTuplingFunctionArgs(index: SemanticdbIndex)
    extends SemanticRule(index, "DottyAutoTuplingFunctionArgs") {
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

  private val ScalaAny = SymbolMatcher.exact(Symbol("_root_.scala.Any#"))
  private def isMaybeAny(name: Term.Name): Boolean =
    (for {
      symbol <- name.symbol
      tpe <- symbol.resultType
    } yield ScalaAny.matches(tpe)).getOrElse(true)

  // See https://github.com/scalacenter/scalafix/issues/521 why we
  // skip the rewrite in this case.
  private def allNamesAreAny(args: List[Pat]): Boolean = args.forall {
    case Pat.Var(name) => isMaybeAny(name)
    case _ => true
  }

  private def canBeAutoTupled(pf: Term.PartialFunction): Boolean =
    pf.cases match {
      case Case(Pat.Tuple(args), None, _) :: Nil =>
        args.forall {
          case Pat.Var(_) => true
          case Pat.Wildcard() => true
          case _ => false
        } && !allNamesAreAny(args)
      case _ => false
    }

  private def canReplaceBracesWithParens(pf: Term.PartialFunction): Boolean =
    pf.pos.startLine == pf.pos.endLine && pf.cases.head.stats.size == 1
}
