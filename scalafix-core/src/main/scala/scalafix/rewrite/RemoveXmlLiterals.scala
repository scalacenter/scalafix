package scalafix
package rewrite

import scala.meta._
import patch.TreePatch.AddGlobalImport

/** Rewrite Xml Literal to Xml Interpolator
  *
  * e.g.
  * {{{
  *   // before:
  *   <div>{ "Hello }</div>
  *
  *   // after:
  *   xml"<div>${ "Hello }</div>"
  * }}}
  */
case object RemoveXmlLiterals extends Rewrite {

  override def rewrite(ctx: RewriteCtx): Patch = {
    object Xml {
      def unapply(tree: Tree): Option[Seq[Lit]] =
        tree match {
          case Pat.Xml(parts, _) => Some(parts)
          case Term.Xml(parts, _) => Some(parts)
          case _ => None
        }
    }

    def isMultiLine(xml: Tree) =
      xml.pos.start.line != xml.pos.end.line

    /** Contains '"' or '\' */
    def containsEscapeSequence(xmlPart: Lit) = {
      val Lit(value: String) = xmlPart
      value.exists(c => c == '\"' || c == '\\')
    }

    /** Rewrite xml literal to interpolator */
    def patchXml(xml: Tree, tripleQuoted: Boolean) = {

      // We don't want to patch inner xml literals multiple times
      def removeSplices(tokens: Tokens) = {
        var depth = 0

        tokens.filter {
          case Token.Xml.SpliceStart() =>
            depth += 1
            depth == 1
          case Token.Xml.SpliceEnd() =>
            depth -= 1
            depth == 0
          case _ =>
            depth == 0
        }
      }

      removeSplices(xml.tokens).collect {
        case tok @ Token.Xml.Start() =>
          val toAdd =
            if (tripleQuoted) "xml\"\"\""
            else "xml\""
          ctx.addLeft(tok, toAdd)

        case tok @ Token.Xml.End() =>
          val toAdd =
            if (tripleQuoted) "\"\"\""
            else "\""
          ctx.addRight(tok, toAdd)

        case tok @ Token.Xml.SpliceStart() =>
          ctx.addLeft(tok, "$")

        case tok @ Token.Xml.Part(part) =>
          if (part.contains('$'))
            ctx.replaceToken(tok, part.replaceAllLiterally("$", "$$"))
          else Patch.empty

      }.asPatch
    }

    /** Emit warning on `{{` */
    def warnOnDoubledBrace(xml: Tree) = xml.tokens.foreach {
      case tok @ Token.Xml.Part(part) if part.contains("{{") =>
        val offset = part.indexOf("{{")
        val pos =
          Position.Range(tok.input, tok.start + offset, tok.start + offset + 2)
        ctx.reporter.warn(
          "Single opening braces within XML text don't need to be doubled",
          pos)
      case _ =>
    }

    val patch = ctx.tree.collect {
      case xml @ Xml(parts) =>
        warnOnDoubledBrace(xml)
        val tripleQuote = isMultiLine(xml) || parts.exists(
          containsEscapeSequence)
        patchXml(xml, tripleQuote)
    }.asPatch

    if (patch.nonEmpty) patch + AddGlobalImport(importer"scala.xml.quote._")
    else patch
  }
}
