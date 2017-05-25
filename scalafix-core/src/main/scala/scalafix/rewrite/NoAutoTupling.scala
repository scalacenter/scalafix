package scalafix
package rewrite

import scala.meta._
import scalafix.syntax._
import scala.meta.semantic.Signature
import scala.collection.mutable

case class NoAutoTupling(mirror: Mirror) extends SemanticRewrite(mirror) {

  private[this] val singleTuplePattern = "^Lscala\\/Tuple\\d{1,2}$".r.pattern

  private[this] def addWrappingParens(ctx: RewriteCtx,
                                      args: Seq[Term.Arg]): Patch =
    ctx.addLeft(args.head.tokens.head, "(") +
      ctx.addRight(args.last.tokens.last, ")")

  override def rewrite(ctx: RewriteCtx): Patch = {
    // "hack" to avoid fixing an argument list more than once due
    // to recursive matching of multiple parameters lists.
    val fixed = mutable.Set.empty[Seq[Term.Arg]]
    ctx.tree.collect {
      case q"${fun: Term.Ref}(...$argss)" if argss.nonEmpty =>
        fun.symbolOpt
          .collect {
            case Symbol.Global(_, Signature.Method(_, jvmSignature)) =>
              val argListSignatures =
                jvmSignature.stripPrefix("(").takeWhile(_ != ')').split(';')
              argListSignatures
                .zip(argss)
                .foldLeft(Patch.empty) {
                  case (patches, (argListSignature, args)) =>
                    if (!fixed(args) && singleTuplePattern
                          .matcher(argListSignature)
                          .matches && args.length > 1) {
                      fixed += args // dirty hack, see explanation above
                      patches + addWrappingParens(ctx, args)
                    } else {
                      patches
                    }
                }
          }
          .getOrElse(Patch.empty)
    }.asPatch
  }
}
