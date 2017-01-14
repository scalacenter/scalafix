package scalafix.util

import scala.meta._
import scala.meta.tokens.Token
import scala.meta.tokens.Token

sealed abstract class Patch {
  def runOn(str: Seq[Token]): Seq[Token]
}

object Patch {
  case class AddLeft(tok: Token, toAdd: String) extends Patch {
    override def runOn(str: Seq[Token]): Seq[Token] = str.flatMap {
      case `tok` => toAdd.tokenize.get.toSeq :+ tok
      case t => List(t)
    }
  }
  case class Replace(from: Token, to: Token, replace: String) extends Patch {
    def insideRange(token: Token): Boolean =
      token.input.eq(from.input) &&
        token.end <= to.end &&
        token.start >= from.start

    val tokens = replace.tokenize.get.tokens.toSeq
    def runOn(str: Seq[Token]): Seq[Token] = {
      str.flatMap {
        case `from` => tokens
        case x if insideRange(x) => Nil
        case x => Seq(x)
      }
    }
  }
  def apply(from: Token, to: Token, replace: String): Patch =
    Replace(from, to, replace)
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
}
