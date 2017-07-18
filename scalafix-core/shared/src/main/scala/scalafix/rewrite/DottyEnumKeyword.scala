package scalafix
package rewrite

import scala.meta.tokens.Token

case object DottyEnumKeyword extends Rewrite {
  override def rewrite(ctx: RewriteCtx): Patch =
    ctx.tokens.collect {
      case enum @ Token.Ident("enum") => ctx.replaceToken(enum, "`enum`")
    }.asPatch
}
