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

sealed abstract class Patch {
  // NOTE: potential bottle-neck, this might be very slow for large
  // patches. We might want to group related patches and enforce some ordering.
  def +(other: Patch): Patch =
    if (this eq other) this
    else {
      (this, other) match {
        case (InCtx(ap, ac, am), b @ InCtx(bp, bc, bm)) =>
          if (ac ne bc) ???
          else InCtx(ap + bp, ac, am.orElse(bm))
        case (_, InCtx(p, ctx, m)) => InCtx(p + this, ctx, m)
        case (InCtx(p, ctx, m), _) => InCtx(p + other, ctx, m)
        case _ => Concat(this, other)
      }
    }
  def ++(other: Seq[Patch]): Patch = other.foldLeft(this)(_ + _)

  def appliedDiff: String = {
    ???
//    val original = ctx.tree.syntax
//    val obtained = applied[T]
//    Patch.unifiedDiff(original, obtained)
  }

  def applied: String = Patch(this)
//    Patch.apply[T](this)
}

private[scalafix] case class Concat(a: Patch, b: Patch) extends Patch
private[scalafix] case object EmptyPatch extends Patch
private[scalafix] case class InCtx(patch: Patch,
                                   ctx: RewriteCtx,
                                   mirror: Option[Mirror])
    extends Patch
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
    case _ =>
      sys.error(s"""Can't merge token patches:
                   |1. $a
                   |2. $b""".stripMargin)
  }
  def apply(patch: Patch): String = patch match {
    case InCtx(p, ctx, Some(mirror)) =>
      semanticApply(underlying(p))(ctx, mirror)
    case InCtx(p, ctx, None) =>
      syntaxApply(ctx, underlying(p).collect { case tp: TokenPatch => tp })
    case _ => ??? // can't support multiple ctx in same patch
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

  def unifiedDiff(a: String, b: String): String = {
    import scala.collection.JavaConverters._
    val originalLines = a.lines.toSeq.asJava
    val diff = DiffUtils.diff(originalLines, b.lines.toSeq.asJava)
    DiffUtils
      .generateUnifiedDiff("original", "revised", originalLines, diff, 3)
      .asScala
      .mkString("\n")
  }
}
