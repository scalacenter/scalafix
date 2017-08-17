package scalafix
package rewrite

import scala.collection.immutable.Seq
import scala.meta._
import scalafix.internal.config.ScalafixMetaconfigReaders
import scalafix.internal.config.ScalafixConfig
import scalafix.syntax._
import metaconfig.ConfDecoder
import metaconfig.Configured

/** A Rewrite is a program that produces a Patch from a scala.meta.Tree. */
abstract class Rewrite(implicit val rewriteName: RewriteName) { self =>

  /** Build patch for a single tree/compilation unit.
    *
    * Override this method to implement a rewrite.
    */
  def rewrite(ctx: RewriteCtx): Patch

  /** Combine this rewrite with another rewrite. */
  final def andThen(other: Rewrite): Rewrite = Rewrite.merge(this, other)

  /** Returns string output of applying this single patch. */
  final def apply(ctx: RewriteCtx): String = apply(ctx, rewrite(ctx))
  final def apply(
      input: Input,
      config: ScalafixConfig = ScalafixConfig.default): String = {
    val ctx = RewriteCtx(config.dialect(input).parse[Source].get, config)
    val patch = rewrite(ctx)
    apply(ctx, patch)
  }
  final def apply(input: String): String = apply(Input.String(input))
  final def apply(ctx: RewriteCtx, patch: Patch): String = {
    val result = Patch(patch, ctx, semanticOption)
    Patch.lintMessages(patch, ctx).foreach { msg =>
      ctx.printLintMessage(msg.copy(id = msg.id.copy(owner = rewriteName)))
    }
    result
  }

  /** Returns unified diff from applying this patch */
  final def diff(ctx: RewriteCtx): String =
    diff(ctx, rewrite(ctx))
  final protected def diff(ctx: RewriteCtx, patch: Patch): String = {
    val original = ctx.tree.input
    Patch.unifiedDiff(
      original,
      Input.VirtualFile(original.label, apply(ctx, patch)))

  }

  final def name: String = rewriteName.toString
  final def names: List[String] = rewriteName.identifiers.map(_.value)
  final override def toString: String = name.toString

  // NOTE. This is kind of hacky and hopefully we can find a better workaround.
  // The challenge is the following:
  // - a.andThen(b) needs to work for mixing semantic + syntactic rewrites.
  // - applied/appliedDiff should work without passing in SemanticCtx explicitly
  protected[scalafix] def semanticOption: Option[SemanticCtx] = None
}

abstract class SemanticRewrite(semanticCtx: SemanticCtx)(
    implicit name: RewriteName)
    extends Rewrite {
  implicit val ImplicitSemanticCtx: SemanticCtx = semanticCtx
  override def semanticOption: Option[SemanticCtx] = Some(semanticCtx)
}

object Rewrite {
  val syntaxRewriteConfDecoder: ConfDecoder[Rewrite] =
    ScalafixMetaconfigReaders.rewriteConfDecoderSyntactic(
      ScalafixMetaconfigReaders.baseSyntacticRewriteDecoder)
  lazy val empty: Rewrite = syntactic(_ => Patch.empty)(RewriteName.empty)
  def emptyConfigured: Configured[Rewrite] = Configured.Ok(empty)
  def emptyFromSemanticCtxOpt(semanticCtx: Option[SemanticCtx]): Rewrite =
    semanticCtx.fold(empty)(emptySemantic)
  def combine(rewrites: Seq[Rewrite]): Rewrite =
    rewrites.foldLeft(empty)(_ andThen _)
  private[scalafix] def emptySemantic(semanticCtx: SemanticCtx): Rewrite =
    semantic(_ => _ => Patch.empty)(RewriteName.empty)(semanticCtx)

  /** Creates a syntactic rewrite. */
  def syntactic(f: RewriteCtx => Patch)(implicit name: RewriteName): Rewrite =
    new Rewrite() {
      override def rewrite(ctx: RewriteCtx): Patch = f(ctx)
    }

  /** Creates a semantic rewrite. */
  def semantic(f: SemanticCtx => RewriteCtx => Patch)(
      implicit rewriteName: RewriteName): SemanticCtx => Rewrite = {
    semanticCtx =>
      new SemanticRewrite(semanticCtx) {
        override def rewrite(ctx: RewriteCtx): Patch = f(semanticCtx)(ctx)
      }
  }

  /** Creates a rewrite that always returns the same patch. */
  def constant(name: String, patch: Patch, semanticCtx: SemanticCtx): Rewrite =
    new SemanticRewrite(semanticCtx)(RewriteName(name)) {
      override def rewrite(ctx: RewriteCtx): Patch = patch
    }

  /** Combine two rewrites into a single rewrite */
  def merge(a: Rewrite, b: Rewrite): Rewrite = {
    new Rewrite()(a.rewriteName + b.rewriteName) {
      override def rewrite(ctx: RewriteCtx): Patch =
        a.rewrite(ctx) + b.rewrite(ctx)
      override def semanticOption: Option[SemanticCtx] =
        (a.semanticOption, b.semanticOption) match {
          case (Some(m1), Some(m2)) =>
            if (m1 ne m2) throw Failure.MismatchingSemanticCtx(m1, m2)
            else Some(m1)
          case (a, b) => a.orElse(b)
        }
    }
  }
}
