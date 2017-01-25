package scalafix.util

import scala.collection.immutable.Seq
import scala.meta._
import scala.meta.tokens.Token
import scala.meta.tokens.Token
import scalafix.ImportsConfig
import scalafix.ScalafixConfig
import scalafix.rewrite.RewriteCtx
import scalafix.util.TokenPatch.Add
import scalafix.util.TokenPatch.Remove
import scalafix.syntax._

sealed abstract class Patch
abstract class TreePatch extends Patch
abstract class TokenPatch(val tok: Token, val newTok: String)
    extends TreePatch {
  override def toString: String =
    s"TokenPatch(${logger.reveal(tok.syntax)}, ${tok.structure}, $newTok)"
}

abstract class ImportPatch(val importer: CanonicalImport) extends TreePatch
object TreePatch {
  case class RemoveGlobalImport(override val importer: CanonicalImport)
      extends ImportPatch(importer)
  case class AddGlobalImport(override val importer: CanonicalImport)
      extends ImportPatch(importer)
}

object TokenPatch {
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
  def apply(ast: Tree, patches: Seq[Patch])(implicit ctx: RewriteCtx): String = {
    val input = ast.tokens
    val tokenPatches = patches.collect { case e: TokenPatch => e }
    val importPatches = OrganizeImports.organizeImports(ast, patches.collect {
      case e: ImportPatch => e
    })
    val patchMap: Map[(Int, Int), String] =
      (importPatches ++ tokenPatches)
        .groupBy(_.tok.posTuple)
        .mapValues(_.reduce(merge).newTok)
    input.toIterator
      .map(x => patchMap.getOrElse(x.posTuple, x.syntax))
      .mkString
  }
}
