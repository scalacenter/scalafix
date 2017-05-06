package scalafix
package rewrite

import scala.meta._
import patch.TreePatch.AddGlobalImport

case object NoXml extends Rewrite {

  override def rewrite(ctx: RewriteCtx): Patch = {
    val patch = ctx.tree.tokens.collect {
      case tok @ Token.Xml.Start() =>
        ctx.addLeft(tok, "xml\"\"\"")

      case tok @ Token.Xml.End() =>
        ctx.addRight(tok, "\"\"\"")

      case tok @ Token.Xml.SpliceStart() =>
        ctx.addLeft(tok, "$")

      case tok @ Token.Xml.Part(part) =>
        if (part.contains('$'))
          ctx.replaceToken(tok, part.replaceAllLiterally("$", "$$"))
        else Patch.empty

    }.asPatch

    // Emit warning on `{{`
    ctx.tree.tokens.foreach {
      case tok @ Token.Xml.Part(part) if part.contains("{{") =>
        val offset = part.indexOf("{{")
        val pos =
          Position.Range(tok.input, tok.start + offset, tok.start + offset + 2)
        ctx.reporter.warn(
          "Single opening braces within XML text don't need to be doubled",
          pos)
      case _ =>
    }

    if (patch.nonEmpty) patch + AddGlobalImport(importer"scala.xml.quote._")
    else patch
  }
}
