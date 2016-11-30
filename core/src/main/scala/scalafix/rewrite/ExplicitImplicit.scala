package scalafix.rewrite

import scala.{meta => m}
import scalafix.util.Patch
import scalafix.util.logger

case object ExplicitImplicit extends Rewrite {

  override def rewrite(ast: m.Tree, ctx: RewriteCtx): Seq[Patch] = {
    import scala.meta._
    val semantic = ctx.semantic.get
    ast.collect {
      case t @ m.Defn.Val(mods, Seq(pat), None, _)
          if mods.exists(_.syntax == "implicit") =>
        semantic
          .typeSignature(t)
          .map { typ =>
            logger.elem(typ)
            val tok = pat.tokens.last
            Patch(tok, tok, s"$tok: ${typ.syntax}")
          }
          .toSeq
    }.flatten
  }
}
