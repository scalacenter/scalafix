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
import scalafix.patch.TreePatch.ImportPatch
import scalafix.patch.TreePatch.RenamePatch
import scalafix.patch.TreePatch.Replace
import scalafix.diff.DiffUtils
import scalafix.internal.patch.ImportPatchOps
import scalafix.internal.patch.Renamer
import scalafix.internal.patch.Replacer
import scalafix.internal.util.TokenOps

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
    else if (isEmpty) other
    else if (other.isEmpty) this
    else Concat(this, other)
  def ++(other: Iterable[Patch]): Patch = other.foldLeft(this)(_ + _)
  def isEmpty: Boolean = this == EmptyPatch
  def nonEmpty: Boolean = !isEmpty
}

//////////////////////////////
// Low-level patches
//////////////////////////////
trait LowLevelPatch
abstract class TokenPatch(val tok: Token, val newTok: String)
    extends Patch
    with LowLevelPatch {
  override def toString: String =
    if (newTok.isEmpty) s"TokenPatch.Remove(${tok.structure.revealWhiteSpace})"
    else
      s"TokenPatch.${this.getClass.getSimpleName}(${tok.syntax.revealWhiteSpace}, ${tok.structure}, $newTok)"
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

//////////////////////////////
// High-level patches
//////////////////////////////
abstract class TreePatch extends Patch
private[scalafix] object TreePatch {
  trait RenamePatch
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
  abstract class ImportPatch extends TreePatch
  @DeriveConfDecoder
  case class RemoveGlobalImport(symbol: Symbol) extends ImportPatch
  @DeriveConfDecoder
  case class RemoveImportee(importee: Importee) extends ImportPatch
  @DeriveConfDecoder
  case class AddGlobalImport(importer: Importer) extends ImportPatch
}

// implementation detail
private[scalafix] case class Concat(a: Patch, b: Patch) extends Patch
private[scalafix] case object EmptyPatch extends Patch with LowLevelPatch

object Patch {

  /** Combine a sequence of patches into a single patch */
  def fromIterable(seq: Iterable[Patch]): Patch =
    seq.foldLeft(empty)(_ + _)

  /** A patch that does no diff/rewrite */
  val empty: Patch = EmptyPatch

  private def merge(a: TokenPatch, b: TokenPatch): TokenPatch = (a, b) match {
    case (add1: Add, add2: Add) =>
      Add(add1.tok,
          add1.addLeft + add2.addLeft,
          add1.addRight + add2.addRight,
          add1.keepTok && add2.keepTok)
    case (_: Remove, add: Add) => add.copy(keepTok = false)
    case (add: Add, _: Remove) => add.copy(keepTok = false)
    case (rem: Remove, rem2: Remove) => rem
    case _ => throw Failure.TokenPatchMergeError(a, b)
  }
  private[scalafix] def apply(p: Patch,
                              ctx: RewriteCtx,
                              mirror: Option[Mirror]): String = {
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
  }

  private def syntaxApply(ctx: RewriteCtx,
                          patches: Iterable[TokenPatch]): String = {
    val patchMap = patches
      .groupBy(x => TokenOps.hash(x.tok))
      .mapValues(_.reduce(merge).newTok)
    ctx.tokens.toIterator
      .map(tok => patchMap.getOrElse(TokenOps.hash(tok), tok.syntax))
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
    val importPatches =
      patches.collect { case e: ImportPatch => e } ++
        replacePatches.collect { case e: ImportPatch => e }
    val importTokenPatches = {
      val result = ImportPatchOps.superNaiveImportPatchToTokenPatchConverter(
        ctx,
        importPatches)
      Patch
        .underlying(result.asPatch)
        .collect {
          case x: TokenPatch => x
          case els =>
            throw Failure.InvariantFailedException(
              s"Expected TokenPatch, got $els")
        }
    }
    val replaceTokenPatches = replacePatches.collect {
      case t: TokenPatch => t
    }
    syntaxApply(
      ctx,
      importTokenPatches ++
        tokenPatches ++
        replaceTokenPatches ++
        renamePatches
    )
  }

  private def underlying(patch: Patch): Seq[Patch] = {
    val builder = Seq.newBuilder[Patch]
    def loop(patch: Patch): Unit = patch match {
      case Concat(a, b) =>
        loop(a)
        loop(b)
      case EmptyPatch => // do nothing
      case els =>
        builder += els
    }
    loop(patch)
    builder.result()
  }

  def unifiedDiff(original: Input, revised: Input): String = {
    import scala.collection.JavaConverters._
    val originalLines = original.asString.lines.toSeq.asJava
    DiffUtils.unifiedDiff(original.label,
                          revised.label,
                          original.asString.lines.toList,
                          revised.asString.lines.toList,
                          3)
  }
}
