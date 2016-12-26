package scalafix.util

import scala.meta._
import scala.meta.tokens.Token
import scala.meta.tokens.Token

/**
  * A patch replaces all tokens between [[from]] and [[to]] with [[replace]].
  */
case class Patch(from: Token, to: Token, replace: String) {
  def insideRange(token: Token): Boolean =
    token.start >= from.start &&
      token.end <= to.end
  val tokens = replace.tokenize.get.tokens.toSeq
  def runOn(str: Seq[Token]): Seq[Token] = {
    str.flatMap {
      case `from` => tokens
      case x if insideRange(x) => Nil
      case x => Seq(x)
    }
  }
}

object Patch {
  def verifyPatches(patches: Seq[Patch]): Unit = {
    // TODO(olafur) assert there's no conflicts.
  }
  def apply(input: Seq[Token], patches: Seq[Patch]): String = {
    verifyPatches(patches)
    // TODO(olafur) optimize, this is SUPER inefficient
    patches
      .foldLeft(input) {
        case (s, p) => p.runOn(s)
      }
      .map(_.syntax)
      .mkString("")
  }

  def replace(token: Token, replacement: String): Patch =
    Patch(token, token, replacement)

  def replace(tree: Tree, replacement: String): Patch =
    Patch(tree.tokens.head, tree.tokens.last, replacement)

  def insertBefore(token: Token, toPrepend: String) =
    replace(token, s"$toPrepend${token.syntax}")

  def insertBefore(tree: Tree, toPrepend: String): Patch =
    replace(tree, s"$toPrepend${tree.syntax}")

  def insertAfter(token: Token, toAppend: String) =
    replace(token, s"$toAppend${token.syntax}")

  def insertAfter(tree: Tree, toAppend: String): Patch =
    replace(tree, s"${tree.syntax}$toAppend")

  def delete(tree: Tree): Patch = replace(tree, "")
}
