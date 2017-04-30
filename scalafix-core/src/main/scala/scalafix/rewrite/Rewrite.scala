package scalafix
package rewrite

import scala.collection.immutable.Seq
import scala.collection.immutable.Seq
import scala.meta._
import scalafix.syntax._
import scalafix.config.ReaderUtil

import metaconfig.ConfDecoder
import sourcecode.Name

/** A rewrite is a named RewriteCtx => Patch function. */
abstract class Rewrite(implicit sourceName: Name) { self =>
  def rewrite(ctx: RewriteCtx): Patch

  def andThen(other: Rewrite): Rewrite = Rewrite.merge(this, other)

  /** Returns unified diff from applying this patch */
  def appliedDiff(ctx: RewriteCtx): String = {
    val original = ctx.tree.input
    Patch.unifiedDiff(original,
                      Input.LabeledString(original.label, applied(ctx)))
  }

  /** Returns string output of applying this single patch.
    *
    * Note. This method may be removed in the future because it makes the assumption
    * that there is only a single RewriteCtx in this Patch. In the future, we may
    * want to support the ability to combine patches from different files to build
    * a unified diff.
    **/
  def applied(ctx: RewriteCtx): String = Patch(rewrite(ctx), ctx, None)
  def name: String = sourceName.value
  override def toString: String = name
}

abstract class SemanticRewrite(mirror: Mirror)(implicit name: Name)
    extends Rewrite {
  implicit val ImplicitMirror: Mirror = mirror
  override def applied(ctx: RewriteCtx): String =
    Patch(rewrite(ctx), ctx, Some(mirror))
}

object Rewrite {
  val syntaxRewriteConfDecoder = config.rewriteConfDecoder(None)
  def empty: Rewrite = syntactic(_ => Patch.empty)
  def combine(rewrites: Seq[Rewrite], mirror: Option[Mirror]): Rewrite =
    rewrites.foldLeft(mirror.fold(empty)(emptySemantic))(_ andThen _)
  // NOTE: this is one example where the Rewrite.wrappedRewrite hack leaks.
  // An empty semantic rewrite is necessary to support patches from .scalafix.conf
  // like `patches.addGlobalImport = ???`.
  // TODO(olafur) get rid of this rewrite by converting `patches.addGlobalImport`
  // into an actual rewrite instead of handling it specially inside Patch.applied.
  private[scalafix] def emptySemantic(mirror: Mirror): Rewrite =
    semantic(x => y => Patch.empty)(Name("empty"))(mirror)
  def syntactic(f: RewriteCtx => Patch)(implicit name: Name): Rewrite =
    new Rewrite() {
      override def rewrite(ctx: RewriteCtx): Patch = f(ctx)
    }
  def semantic(f: Mirror => RewriteCtx => Patch)(
      implicit name: Name): Mirror => Rewrite = { mirror =>
    new SemanticRewrite(mirror) {
      override def rewrite(ctx: RewriteCtx): Patch = f(mirror)(ctx)
    }
  }
  def merge(a: Rewrite, b: Rewrite): Rewrite = {
    val newName =
      if (a.name == "empty") b.name
      else if (b.name == "empty") a.name
      else s"${a.name}+${b.name}"
    new Rewrite()(Name(newName)) {
      override def rewrite(ctx: RewriteCtx) =
        a.rewrite(ctx) + b.rewrite(ctx)
      override def wrappedRewrite(ctx: RewriteCtx): Patch =
        a.wrappedRewrite(ctx) + b.wrappedRewrite(ctx)
    }
  }
}
