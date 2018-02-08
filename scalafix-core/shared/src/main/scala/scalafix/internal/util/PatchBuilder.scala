package scalafix.internal.util

import scala.meta.{Tokens, Token}
import scala.meta.internal.tokens.TokenInfo
import scalafix.Patch
import scalafix.rule.RuleCtx

class PatchBuilder(tokens: Tokens, ctx: RuleCtx) {
  private val iterator = new PeekableIterator(tokens.iterator)
  private val patches = List.newBuilder[Patch]
  def next: Token = iterator.next
  def find[T <: Token: TokenInfo]: Option[Token] =
    iterator.find(_.is[T](implicitly[TokenInfo[T]]))
  def find(f: Token => Boolean): Option[Token] =
    iterator.find(f)
  def remove[T <: Token: TokenInfo]: Unit =
    doRemove(find[T])
  def remove(t: Token): Unit =
    patches += ctx.removeToken(t)
  def removeOptional[T <: Token: TokenInfo]: Unit =
    if (iterator.peek.forall(_.is[T])) remove[T]
  def removeLast[T <: Token: TokenInfo]: Unit = {
    var last: Option[Token] = None
    while (iterator.hasNext) {
      last = find[T]
    }
    doRemove(last)
  }
  def addRight[T <: Token: TokenInfo](toAdd: String): Unit =
    doOp(find[T], tt => patches += ctx.addRight(tt, toAdd))
  def addLeft(t: Token, what: String): Unit =
    patches += ctx.addLeft(t, what)
  def doRemove[T <: Token](t: Option[Token])(
      implicit ev: TokenInfo[T]): Unit =
    doOp(t, tt => patches += ctx.removeToken(tt))
  def doOp[T <: Token](t: Option[Token], op: Token => Unit)(
      implicit ev: TokenInfo[T]): Unit =
    t match {
      case Some(t) => op(t)
      case _ =>
        throw new Exception(
          s"""|cannot find ${ev.name}
              |Tokens:
              |$tokens""".stripMargin
        )
    }
  def result(): Patch = Patch.fromIterable(patches.result())
}