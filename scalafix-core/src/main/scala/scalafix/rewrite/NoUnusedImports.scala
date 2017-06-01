package scalafix
package rewrite

import scala.meta._
import scala.annotation.tailrec

case class NoUnusedImports(mirror: Mirror) extends SemanticRewrite(mirror) {

  @tailrec
  private[this] def removeTrailingWhitespacesOrSemi(
      ctx: RewriteCtx,
      t: Token,
      patch: Patch = Patch.empty): Patch = {
    val nextToken = ctx.tokenList.next(t)
    if (nextToken.is[Token.LF] || nextToken.is[Token.Semicolon]) {
      patch + ctx.removeToken(nextToken)
    } else if (nextToken.is[Token.Space] || nextToken.is[Token.Tab]) {
      removeTrailingWhitespacesOrSemi(ctx,
                                      nextToken,
                                      patch + ctx.removeToken(nextToken))
    } else {
      patch
    }
  }

  @tailrec
  private[this] def removeLeadingWhitespaces(
      ctx: RewriteCtx,
      t: Token,
      patch: Patch = Patch.empty): Patch = {
    val prevToken = ctx.tokenList.prev(t)
    if (prevToken.is[Token.Space] || prevToken.is[Token.Tab]) {
      removeLeadingWhitespaces(ctx,
                               prevToken,
                               patch + ctx.removeToken(prevToken))
    } else {
      patch
    }
  }

  override def rewrite(ctx: RewriteCtx): Patch = {
    val patches = mirror.database.messages.collect {
      case Message(Anchor(_, start, end), _, "Unused import") =>
        val toRemove =
          ctx.tokens.dropWhile(_.start < start).takeWhile(_.end <= end)

        val removeImport = toRemove.map(ctx.removeToken).asPatch
        removeImport +
          removeLeadingWhitespaces(ctx, toRemove.head) +
          removeTrailingWhitespacesOrSemi(ctx, toRemove.last)
    }
    patches.asPatch
  }
}
