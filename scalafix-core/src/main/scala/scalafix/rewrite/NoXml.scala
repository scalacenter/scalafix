package scalafix
package rewrite

import scala.meta._

import org.scalameta.logger
case object NoXml extends Rewrite {
  override def rewrite(ctx: RewriteCtx): Patch = {
    ctx.tree.tokens.foreach(token =>
      logger.elem(token.structure, token.getClass))
    ctx.tree.tokens.collect {
      case tok @ Token.Xml.Start() =>
        ctx.addLeft(tok, "xml\"\"\"")
      case tok @ Token.Xml.End() =>
        ctx.addRight(tok, "\"\"\"")
      case tok @ Token.Xml.SpliceStart() =>
        ctx.addRight(tok, "$")
    }.asPatch
  }
}

