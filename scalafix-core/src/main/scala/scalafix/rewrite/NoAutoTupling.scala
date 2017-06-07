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

  private[this] def fixMethods(ctx: RewriteCtx,
                               jvmSignature: String,
                               argss: Seq[Seq[Term.Arg]],
                               fixed: mutable.Set[Seq[Term.Arg]]): Patch = {
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

  private[this] def fixFunctions(ctx: RewriteCtx,
                                 symbol: Symbol,
                                 argss: Seq[Seq[Term.Arg]],
                                 fixed: mutable.Set[Seq[Term.Arg]],
                                 mirror: Mirror): Patch =
    mirror.database.denotations
      .get(symbol)
      .map { denot =>
        val argListSignatures =
          denot.info.split("=>").map(_.trim).toList.dropRight(1)
        argListSignatures
          .zip(argss)
          .foldLeft(Patch.empty) {
            case (patches, (argListSignature, args)) =>
              if (!fixed(args) && argListSignature.startsWith("((") && argListSignature
                    .endsWith("))")
                  && args.length > 1) {
                fixed += args
                patches + addWrappingParens(ctx, args)
              } else {
                patches
              }
          }
      }
      .asPatch

  override def rewrite(ctx: RewriteCtx): Patch = {
    // "hack" to avoid fixing an argument list more than once due
    // to recursive matching of multiple parameters lists.
    val fixed = mutable.Set.empty[Seq[Term.Arg]]
    ctx.tree.collect {
      case q"${fun: Term.Ref}(...$argss)" if argss.nonEmpty =>
        fun.symbolOpt.collect {
          case s @ Symbol.Global(_, Signature.Method(_, jvmSignature)) =>
            fixMethods(ctx, jvmSignature, argss, fixed) +
              fixFunctions(ctx, s, argss, fixed, mirror)

          case s @ Symbol.Global(_, Signature.Term(x))
              if mirror.database.denotations.get(s).isDefined =>
            fixFunctions(ctx, s, argss, fixed, mirror)
        }.asPatch
    }.asPatch
  }
}
