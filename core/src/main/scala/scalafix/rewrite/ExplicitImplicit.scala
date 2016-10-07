package scalafix.rewrite

import scala.meta._
import scala.meta.internal.prettyprinters.Attributes
import scala.meta.tokens.Token.Comment
import scala.meta.tokens.Token.LeftBrace
import scala.meta.tokens.Token.RightBrace
import scala.meta.tokens.Token.RightParen
import scala.meta.tokens.Token.Space
import scalafix.util.Patch
import scalafix.util.SemanticOracle
import scalafix.util.logger

case object ExplicitImplicit extends Rewrite {

  override def rewrite(ast: Tree, ctx: RewriteCtx): Seq[Patch] = {
    val builder = Seq.newBuilder[Patch]
    val oracle = new SemanticOracle(ctx.mirror.get)
    ast.collect {
      case Defn.Val(mods, Seq(Pat.Var.Term(t: Term.Name)), decltpe, _)
          if decltpe.isEmpty && mods.exists(_.syntax == "implicit") =>
        oracle.getType(t).foreach { typ =>
          val toks = t.tokens
          builder += Patch(toks.head, toks.last, s"${t.syntax}: $typ")
        }
    }
    builder.result()
  }
}
