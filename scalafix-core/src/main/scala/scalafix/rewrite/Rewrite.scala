package scalafix
package rewrite

import scala.collection.immutable.Seq
import scala.collection.immutable.Seq
import scala.meta._
import scalafix.config.ReaderUtil

import metaconfig.ConfDecoder
import sourcecode.Name

/** A rewrite is a named RewriteCtx => Patch function. */
abstract class Rewrite(implicit sourceName: Name) { self =>

  def name: String = sourceName.value
  override def toString: String = name
  def rewrite(ctx: RewriteCtx): Patch
  def andThen(other: Rewrite): Rewrite = Rewrite.merge(this, other)
  // NOTE: See comment for InCtx for an explanation why wrappedRewrite and
  // InCtx are necessary.
  private[scalafix] def wrappedRewrite(ctx: RewriteCtx): Patch =
    patch.InCtx(rewrite(ctx), ctx, None)
}

abstract class SemanticRewrite(mirror: Mirror)(implicit name: Name)
    extends Rewrite {
  implicit val ImplicitMirror: Mirror = mirror
  private[scalafix] override def wrappedRewrite(ctx: RewriteCtx): Patch =
    patch.InCtx(rewrite(ctx), ctx, Some(mirror))
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
