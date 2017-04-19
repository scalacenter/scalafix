// scalafmt: { maxColumn = 120, style = defaultWithAlign }
package scalafix
package rewrite

import scala.collection.immutable.Seq
import scala.meta._
import scala.meta.semantic.v1.Completed
import scala.meta.semantic.v1.Mirror
import scala.meta.semantic.v1.Signature
import scala.meta.semantic.v1.Symbol

case class Scalameta17(implicit mirror: Mirror) extends SemanticRewrite(mirror) {

  override def rewrite(ctx: RewriteCtx): Patch = {
    import ctx._
    val litPatches = tree.collect {
      case t @ Term.Name("Lit") if t.symbol.syntax.startsWith("_root_.scala.meta.Lit.apply(") =>
        val tok = t.tokens.head

        val Symbol.Global(_, Signature.Method(_, jvm)) = t.symbol
        jvm match {
          case "(C)Lscala/meta/Lit;"                         => ctx.addRight(tok, ".Char")
          case "(D)Lscala/meta/Lit;"                         => ctx.addRight(tok, ".Double")
          case "(F)Lscala/meta/Lit;"                         => ctx.addRight(tok, ".Float")
          case "(I)Lscala/meta/Lit;"                         => ctx.addRight(tok, ".Int")
          case "(J)Lscala/meta/Lit;"                         => ctx.addRight(tok, ".Long")
          case "(S)Lscala/meta/Lit;"                         => ctx.addRight(tok, ".Short")
          case "(Z)Lscala/meta/Lit;"                         => ctx.addRight(tok, ".Boolean")
          case "(Ljava/lang/String;)Lscala/meta/Lit;"        => ctx.addRight(tok, ".String")
          case "(Lscala/Symbol;)Lscala/meta/Lit;"            => ctx.addRight(tok, ".Symbol")
          case "(Lscala/runtime/BoxedUnit;)Lscala/meta/Lit;" => ctx.addRight(tok, ".Unit")
          case _                                             => ctx.addRight(tok, "") // nothing
        }
    }
    ctx.renameSymbol(Symbol("_root_.scala.meta.io.AbsolutePath#getAbsolutePath()Ljava/lang/String;."), q"absolute") ++
      litPatches
  }
}
