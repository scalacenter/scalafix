package scalafix
package rewrite

import scala.meta._
import scalafix.syntax._
import scala.meta.semantic.Signature
import scala.collection.mutable.{Set => MutableSet}

case class NoAutoTupling(mirror: Mirror) extends SemanticRewrite(mirror) {

  private[this] val singleTuplePattern = "^Lscala\\/Tuple\\d{1,2}$".r.pattern

  private[this] def addWrappingParens(ctx: RewriteCtx,
                                      args: Seq[Term.Arg]): Patch =
    Seq(
      ctx.addLeft(args.head.tokens.last, "("),
      ctx.addRight(args.last.tokens.last, ")")
    ).asPatch

  override def rewrite(ctx: RewriteCtx): Patch = {
    // "hack" to avoid fixing an argument list more than once due
    // to recursive matching of multiple parameters lists.
    val fixed = MutableSet.empty[Seq[Term.Arg]]
    ctx.tree.collect {
      case x @ q"$fun(...$argss)"
          if argss.length > 0 && fun.isInstanceOf[Ref] =>
        mirror
          .symbol(fun.asInstanceOf[Ref])
          .toOption
          .collect {
            case Symbol.Global(_, Signature.Method(_, jvmSignature)) =>
              jvmSignature.stripPrefix("(").takeWhile(_ != ')').split(';')
          }
          .map { argListSignatures =>
            argListSignatures
              .zip(argss)
              .foldLeft(Seq.empty[Patch]) {
                case (patches, (argListSignature, args)) =>
                  if (!fixed(args) && singleTuplePattern
                        .matcher(argListSignature)
                        .matches && args.length > 1) {
                    fixed += args // dirty hack, see explanation above
                    patches :+ addWrappingParens(ctx, args)
                  } else {
                    patches
                  }
              }
              .asPatch
          }
          .getOrElse(Patch.empty)
    }.asPatch
  }
}
