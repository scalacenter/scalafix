// scalafmt: { maxColumn = 120, style = defaultWithAlign }
package scalafix.rewrite

import scala.collection.immutable.Seq
import scala.meta._
import scala.meta.semantic.v1.Completed
import scala.meta.semantic.v1.Mirror
import scala.meta.semantic.v1.Signature
import scala.meta.semantic.v1.Symbol
import scalafix.util.Patch
import scalafix.util.TokenPatch._
import scalafix.util.TreePatch.RenameSymbol

case object Scalameta17 extends Rewrite[Mirror] {

  override def rewrite[T <: Mirror](ctx: RewriteCtx[T]): Patch = {
    import ctx._
    val litPatches = tree.collect {
      case t @ Term.Name("Lit") if t.symbol.syntax.startsWith("_root_.scala.meta.Lit.apply(") =>
        val tok = t.tokens.head

        val Symbol.Global(_, Signature.Method(_, jvm)) = t.symbol
        jvm match {
          case "(C)Lscala/meta/Lit;"                         => AddRight(tok, ".Char")
          case "(D)Lscala/meta/Lit;"                         => AddRight(tok, ".Double")
          case "(F)Lscala/meta/Lit;"                         => AddRight(tok, ".Float")
          case "(I)Lscala/meta/Lit;"                         => AddRight(tok, ".Int")
          case "(J)Lscala/meta/Lit;"                         => AddRight(tok, ".Long")
          case "(S)Lscala/meta/Lit;"                         => AddRight(tok, ".Short")
          case "(Z)Lscala/meta/Lit;"                         => AddRight(tok, ".Boolean")
          case "(Ljava/lang/String;)Lscala/meta/Lit;"        => AddRight(tok, ".String")
          case "(Lscala/Symbol;)Lscala/meta/Lit;"            => AddRight(tok, ".Symbol")
          case "(Lscala/runtime/BoxedUnit;)Lscala/meta/Lit;" => AddRight(tok, ".Unit")
          case _                                             => AddRight(tok, "") // nothing
        }
    }
    RenameSymbol(Symbol("_root_.scala.meta.io.AbsolutePath#getAbsolutePath()Ljava/lang/String;."), q"absolute") ++
      litPatches
  }
}
