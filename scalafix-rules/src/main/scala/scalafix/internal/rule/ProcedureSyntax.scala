package scalafix.internal.rule

import scala.meta._
import scala.meta.tokens.Token
import scala.meta.tokens.Token.Equals
import scala.meta.tokens.Token.RightParen

import scalafix.util.Trivia
import scalafix.v1._

class ProcedureSyntax extends SyntacticRule("ProcedureSyntax") {

  override def description: String =
    "Replaces deprecated procedure syntax with explicit ': Unit ='"

  override def isRewrite: Boolean = true

  override def fix(implicit doc: SyntacticDocument): Patch = {
    doc.tree.collect {
      case t: Decl.Def if t.decltpe.tokens.isEmpty =>
        Patch.addRight(t.tokens.last, s": Unit").atomic
      case t: Defn.Def
          if t.decltpe.exists(_.tokens.isEmpty) &&
            t.body.tokens.head.is[Token.LeftBrace] =>
        val fixed = for {
          bodyStart <- t.body.tokens.headOption
          toAdd <- doc.tokenList.leading(bodyStart).find(!_.is[Trivia])
        } yield Patch.addRight(toAdd, s": Unit =").atomic
        fixed.getOrElse(Patch.empty)

      /** @see [[https://github.com/ohze/scala-rewrites/blob/dotty/rewrites/src/main/scala/fix/scala213/ConstructorProcedureSyntax.scala ConstructorProcedureSyntax.scala]] */
      case t: Ctor.Secondary =>
        val tokens = t.tokens
        val beforeInitIdx = tokens.indexOf(t.init.tokens.head) - 1
        // last RightParen before init
        val lastRightParenIdx =
          tokens.lastIndexWhere(_.is[RightParen], beforeInitIdx)
        // if slicedTokens don't have Equals token => need patching
        val slicedTokens = tokens.slice(lastRightParenIdx, beforeInitIdx)
        slicedTokens.find(_.is[Equals]) match {
          case Some(_) => Patch.empty
          case None => Patch.addRight(tokens(lastRightParenIdx), " =")
        }

    }.asPatch
  }
}
