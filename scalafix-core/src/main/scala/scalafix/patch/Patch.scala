package scalafix
package patch

import metaconfig._
import scala.collection.immutable.Seq
import scala.meta._
import scala.meta.contrib._
import scala.meta.internal.ast.Helpers._
import scala.meta.tokens.Token
import scala.meta.tokens.Token
import scalafix.config._
import scalafix.syntax._
import scalafix.patch.TokenPatch.Add
import scalafix.patch.TokenPatch.Remove
import scalafix.patch.TreePatch.RenamePatch
import scalafix.patch.TreePatch.Replace

import difflib.DiffUtils
import org.scalameta.logger

/** A data structure that can produce a .patch file.
  *
  * The best way to build a Patch is with a RewriteCtx inside a Rewrite.
  * For example, `Rewrite.syntactic(ctx => ctx.addLeft(ctx.tree.tokens.head): Patch)`
  *
  * Patches can be composed with Patch.+ and Patch.++. A Seq[Patch] can be combined
  * into a single patch with `Seq[Patch](...).asPatch` with `import scalafix._`.
  *
  * Patches are split into low-level token patches and high-level tree patches.
  * A token patch works on scala.meta.Token and provides surgical precision over
  * how details like formatting are managed by the rewrite.
  *
  * NOTE: Patch current only works for a single file, but it may be possible
  * to add support in the future for combining patches for different files
  * with Patch + Patch.
  */
sealed abstract class Patch {
  // NOTE: potential bottle-neck, this might be very slow for large
  // patches. We might want to group related patches and enforce some ordering.
  def +(other: Patch): Patch =
    if (this eq other) this
    else {
      (this, other) match {
        case (a: InCtx, b: InCtx) => InCtx.merge(a, b)
        case _ => Concat(this, other)
      }
    }
  def ++(other: Seq[Patch]): Patch = other.foldLeft(this)(_ + _)

  /** Returns unified diff from applying this patch */
  def appliedDiff: String = {
    val ctx = this match {
      case InCtx(_, ctx, _) => ctx
      case _ => throw Failure.MissingTopLevelInCtx(this)
    }
    val original = ctx.tree.input
    Patch.unifiedDiff(original, Input.LabeledString(original.label, applied))
  }

  /** Returns string output of applying this single patch.
    *
    * Note. This method may be removed in the future because it makes the assumption
    * that there is only a single RewriteCtx in this Patch. In the future, we may
    * want to support the ability to combine patches from different files to build
    * a unified diff.
    **/
  def applied: String = Patch(this)
}

private[scalafix] case class Concat(a: Patch, b: Patch) extends Patch
private[scalafix] case object EmptyPatch extends Patch
// NOTE: This method is an implementation detail of how Patch works internally.
// The challenge is that patches need a Mirror and RewriteCtx, but I don't
// want Patch.applied/appliedDiff to to accept a Mirror or RewriteCtx argument.
// The public api now forces users to do for example `ctx.addGlobalImport`
// so we know that the ctx and mirror are available when the user calls that method.
// However, I don't want to store the mirror and ctx with every Patch.
// wrappedRewrite is a hack to store the ctx and mirror only once per source
// file at the top of the Patch. This detail may change in the future if
// we can come up with a less hacky and more robust way to accomplish the same.
// However, by keeping this method private, the public api should can remain unchanged
// if we change this implementation detail.
private[scalafix] case class InCtx(patch: Patch,
                                   ctx: RewriteCtx,
                                   mirror: Option[Mirror])
    extends Patch
private[scalafix] object InCtx {
  def merge(a: InCtx, b: InCtx): Patch = (a, b) match {
    case (InCtx(_, ac, _), InCtx(_, bc, _)) if ac ne bc =>
      // TODO(olafur) Encode failure inside a patch to support accummulation of errors
      // instead of fail-fast exceptions.
      throw Failure.MismatchingRewriteCtx(ac, bc)
    case (InCtx(_, _, Some(am)), InCtx(_, _, Some(bm))) if am ne bm =>
      throw Failure.MismatchingMirror(am, bm)
    case (InCtx(ap, ac, am), InCtx(bp, bc, bm)) =>
      InCtx(ap + bp, ac, am.orElse(bm))
    case (_, InCtx(p, ctx, m)) => InCtx(p + a, ctx, m)
    case (InCtx(p, ctx, m), _) => InCtx(p + b, ctx, m)
  }
}
abstract class TreePatch extends Patch
abstract class TokenPatch(val tok: Token, val newTok: String) extends Patch {
  override def toString: String =
    s"TokenPatch(${tok.syntax.revealWhiteSpace}, ${tok.structure}, $newTok)"
}

abstract class ImportPatch(val importer: Importer) extends TreePatch {
  def importee: Importee = importer.importees.head
  def toImport: Import = Import(Seq(importer))
}

private[scalafix] object TreePatch {
  trait RenamePatch
  case class Rename(from: Name, to: Name) extends TreePatch with RenamePatch
  case class RenameSymbol(from: Symbol, to: Name, normalize: Boolean = false)
      extends TreePatch
      with RenamePatch {
    private lazy val resolvedFrom = if (normalize) from.normalized else from
    def matches(symbol: Symbol): Boolean =
      if (normalize) symbol.normalized == resolvedFrom
      else symbol == resolvedFrom
  }

  @DeriveConfDecoder
  case class Replace(from: Symbol,
                     to: Term.Ref,
                     additionalImports: List[Importer] = Nil,
                     normalized: Boolean = true)
      extends TreePatch {
    require(to.isStableId)
  }
  case class RemoveGlobalImport(override val importer: Importer)
      extends ImportPatch(importer)
  case class AddGlobalImport(override val importer: Importer)
      extends ImportPatch(importer)
}

private[scalafix] object TokenPatch {
  case class Remove(override val tok: Token) extends TokenPatch(tok, "")
  case class Add(override val tok: Token,
                 addLeft: String,
                 addRight: String,
                 keepTok: Boolean = true)
      extends TokenPatch(tok,
                         s"""$addLeft${if (keepTok) tok else ""}$addRight""")

}
object Patch {
  def fromSeq(seq: scala.Seq[Patch]): Patch = seq.foldLeft(empty)(_ + _)
  def empty: Patch = EmptyPatch
  def merge(a: TokenPatch, b: TokenPatch): TokenPatch = (a, b) match {
    case (add1: Add, add2: Add) =>
      Add(add1.tok,
          add1.addLeft + add2.addLeft,
          add1.addRight + add2.addRight,
          add1.keepTok && add2.keepTok)
    case (_: Remove, add: Add) => add.copy(keepTok = false)
    case (add: Add, _: Remove) => add.copy(keepTok = false)
    case (rem: Remove, _: Remove) => rem
    case _ => throw Failure.TokenPatchMergeError(a, b)
  }
  def apply(patch: Patch): String = patch match {
    case InCtx(p, ctx, mirror) =>
      val patches = underlying(p)
      val semanticPatches = patches.collect { case tp: TreePatch => tp }
      mirror match {
        case Some(x) =>
          semanticApply(underlying(p))(ctx, x)
        case None =>
          if (semanticPatches.nonEmpty)
            throw Failure.Unsupported(
              s"Semantic patches are not supported without a Mirror: $semanticPatches")
          syntaxApply(ctx, underlying(p).collect {
            case tp: TokenPatch => tp
          })
      }

    case _ => throw Failure.MissingTopLevelInCtx(patch)
  }

  private def syntaxApply(ctx: RewriteCtx, patches: Seq[TokenPatch]): String = {
    val patchMap: Map[(Int, Int), String] = patches
      .groupBy(_.tok.posTuple)
      .mapValues(_.reduce(merge).newTok)
    ctx.tokens.toIterator
      .map(x => patchMap.getOrElse(x.posTuple, x.syntax))
      .mkString

  }
  private def semanticApply(patches: Seq[Patch])(implicit ctx: RewriteCtx,
                                                 mirror: Mirror): String = {
    val ast = ctx.tree
    val tokenPatches = patches.collect { case e: TokenPatch => e }
    val renamePatches = Renamer.toTokenPatches(patches.collect {
      case e: RenamePatch => e
    })
    val replacePatches = Replacer.toTokenPatches(ast, patches.collect {
      case e: Replace => e
    })
    val importPatches = OrganizeImports.organizeImports(
      patches.collect { case e: ImportPatch => e } ++
        replacePatches.collect { case e: ImportPatch => e }
    )
    val replaceTokenPatches = replacePatches.collect {
      case t: TokenPatch => t
    }
    syntaxApply(
      ctx,
      importPatches ++
        tokenPatches ++
        replaceTokenPatches ++
        renamePatches
    )
  }

  private def underlying(patch: Patch): Seq[Patch] = {
    val builder = Seq.newBuilder[Patch]
    def loop(patch: Patch): Unit = patch match {
      case InCtx(p, _, _) =>
        loop(p)
      case Concat(a, b) =>
        loop(a)
        loop(b)
      case els =>
        builder += els
    }
    loop(patch)
    builder.result()
  }

  def unifiedDiff(original: Input, revised: Input): String = {
    import scala.collection.JavaConverters._
    val originalLines = original.asString.lines.toSeq.asJava
    val diff =
      DiffUtils.diff(originalLines, revised.asString.lines.toSeq.asJava)
    DiffUtils
      .generateUnifiedDiff(original.label,
                           revised.label,
                           originalLines,
                           diff,
                           3)
      .asScala
      .mkString("\n")
  }
}
