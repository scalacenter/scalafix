package scalafix.internal.rule

import scala.meta._
import scalafix.v1._
import scalafix.util.Trivia

class ProcedureSyntax extends SyntacticRule("ProcedureSyntax") {

  override def description: String =
    "Rewrite that inserts explicit : Unit = for soon-to-be-deprecated procedure syntax def foo { ... }"

  override def fix(implicit doc: Doc): Patch = {
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
    }.asPatch
  }
}
