package scalafix.internal.rule

import scala.meta._

import scalafix.v1._

class ProcedureSyntax extends SyntacticRule("ProcedureSyntax") {

  override def description: String =
    "Replaces deprecated Scala 2.x procedure syntax with explicit ': Unit ='"

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
          toAdd <- doc.tokenList.leading(bodyStart).find(!_.is[Token.Trivia])
        } yield Patch.addRight(toAdd, s": Unit =").atomic
        fixed.getOrElse(Patch.empty)

      /**
       * @see
       *   [[https://github.com/ohze/scala-rewrites/blob/dotty/rewrites/src/main/scala/fix/scala213/ConstructorProcedureSyntax.scala ConstructorProcedureSyntax.scala]]
       */
      case t: Ctor.Secondary =>
        t.body.tokens.rfindWideNot(_.is[Token.Trivia], -1).get match {
          case _: Token.Equals => Patch.empty
          case tok => Patch.addRight(tok, " =")
        }
    }.asPatch
  }
}
