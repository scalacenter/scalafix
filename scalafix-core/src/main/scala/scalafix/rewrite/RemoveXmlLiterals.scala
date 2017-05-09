package scalafix
package rewrite

import scala.meta._

/** Rewrite Xml Literal to Xml Interpolator
  *
  * e.g.
  * {{{
  *   // before:
  *   <div>{ "Hello" }</div>
  *
  *   // after:
  *   xml"<div>${ "Hello" }</div>"
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

      /** Substitute {{ by { */
      def patchEscapedBraces(tok: Token.Xml.Part) = {
        ctx.reporter.warn(
          """Single opening braces don't need be escaped with {{ inside the xml interpolator,
            |unlike xml literals. For example <x>{{</x> is identical to xml"<x>{</x>".
            |This Rewrite will replace all occurrences of {{. Make sure this is intended.
          """.stripMargin,
          tok.pos
        )
        ctx.replaceToken(tok, tok.value.replaceAllLiterally("{{", "{"))
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
          var patch = Patch.empty
          if (part.contains('$'))
            patch += ctx.replaceToken(tok, part.replaceAllLiterally("$", "$$"))
          if (part.contains("{{"))
            patch += patchEscapedBraces(tok)
          patch

      }.asPatch
    }

    /** add `import scala.xml.quote._` */
    def importXmlQuote = {
      val nextToken = {
        def loop(tree: Tree): Token = tree match {
          case Source(stat :: _) => loop(stat)
          case Pkg(_, stat :: _) => loop(stat)
          case els => els.tokens.head
        }
        loop(ctx.tree)
      }

      ctx.addLeft(nextToken, "import scala.xml.quote._\n")
    }

    val patch = ctx.tree.collect {
      case xml @ Xml(parts) =>
        val tripleQuoted = isMultiLine(xml) || parts.exists(
          containsEscapeSequence)
        patchXml(xml, tripleQuoted)
    }.asPatch

    if (patch.nonEmpty) patch + importXmlQuote
    else patch
  }
}
