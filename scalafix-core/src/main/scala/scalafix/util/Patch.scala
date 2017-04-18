package scalafix.util

import metaconfig._
import scala.collection.immutable.Seq
import scala.meta._
import scala.meta.contrib._
import scala.meta.internal.ast.Helpers._
import scala.meta.tokens.Token
import scala.meta.tokens.Token
import scalafix.config._
import scalafix.rewrite.RewriteCtx
import scalafix.syntax._
import scalafix.util.TokenPatch.Add
import scalafix.util.TokenPatch.Remove
import scalafix.util.TreePatch.RenamePatch
import scalafix.util.TreePatch.Replace

sealed abstract class Patch {
  final private[scalafix] def underlying: Seq[Patch] = {
    val builder = Seq.newBuilder[Patch]
    def loop(patch: Patch): Unit = patch match {
      case Concat(a, b) =>
        loop(a)
        loop(b)
      case els =>
        builder += els
    }
    loop(this)
    builder.result()
  }
  // NOTE: potential bottle-neck, this might be very slow for large
  // patches. We might want to group related patches and enforce some ordering.
  def +(other: Patch): Patch = Concat(this, other)
  def ++(other: Seq[Patch]): Patch = other.foldLeft(this)(_ + _)
}

private[scalafix] case class Concat(a: Patch, b: Patch) extends Patch
private[scalafix] case object EmptyPatch extends Patch
abstract class TreePatch extends Patch
abstract class TokenPatch(val tok: Token, val newTok: String)
    extends TreePatch {
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
  @metaconfig.DeriveConfDecoder
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
  def AddRight(tok: Token, toAdd: String): TokenPatch = Add(tok, "", toAdd)
  def AddLeft(tok: Token, toAdd: String): TokenPatch = Add(tok, toAdd, "")
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
  def apply[T <: Mirror: CanOrganizeImports](patch: Patch)(
      implicit ctx: RewriteCtx[T]): String = {
    if (ctx.config.debug.printSymbols)
      ctx.reporter.info(ctx.mirror.database.toString())
    val patches = patch.underlying
    val ast = ctx.tree
    val input = ctx.tokens
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
    val patchMap: Map[(Int, Int), String] =
      (importPatches ++ tokenPatches ++ replaceTokenPatches ++ renamePatches)
        .groupBy(_.tok.posTuple)
        .mapValues(_.reduce(merge).newTok)
    input.toIterator
      .map(x => patchMap.getOrElse(x.posTuple, x.syntax))
      .mkString
  }
}
