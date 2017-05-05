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

    //TODO: emit warning on `{{`

    if (patch.nonEmpty) patch + AddGlobalImport(importer"scala.xml.quote._")
    else patch
  }
}
