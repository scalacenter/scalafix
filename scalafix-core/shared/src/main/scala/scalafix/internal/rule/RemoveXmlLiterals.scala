package scalafix.internal.rule

import scalafix.v0._
import scala.meta._
import scalafix.Patch
import scalafix.rule.Rule
import scalafix.rule.RuleCtx
import scalafix.lint.LintCategory

/* Rule Xml literals to Xml interpolators.
 *
 * e.g.
 * {{{
 *   // before:
 *   <div>{ "Hello" }</div>
 *
 *   // after:
 *   import scala.xml.quote._
 *   xml"<div>${ "Hello" }</div>"
 * }}}
 *
 * This only rules xml literals in expression position:
 * Xml patterns will not be supported by the xml interpolator,
 * until we know how to rule `case <a>{ns @ _*}</a>`.
 */
case object RemoveXmlLiterals extends Rule("RemoveXmlLiterals") {

  override def description: String =
    "Rewrite that converts xml literals into interpolators for use with densh/scala-xml-quote"

  val singleBracesEscape: LintCategory = LintCategory.warning(
    "singleBracesEscape",
    """Single braces don't need be escaped with {{ and }} inside xml interpolators, unlike xml literals.
      |For example <x>{{</x> is identical to xml"<x>{</x>". This Rule will replace all occurrences of
      |{{ and }}. Make sure this is intended.
      |""".stripMargin
  )

  override def fix(ctx: RuleCtx): Patch = {

    def isMultiLine(xml: Term.Xml) =
      xml.pos.startLine != xml.pos.endLine

    /* Contains '"' or '\' */
    def containsEscapeSequence(xmlPart: Lit) = {
      val Lit(value: String) = xmlPart
      value.exists(c => c == '\"' || c == '\\')
    }

    /* Rule xml literal to interpolator */
    def patchXml(xml: Term.Xml, tripleQuoted: Boolean) = {

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

      /* Substitute {{ by { and }} by } */
      def patchEscapedBraces(tok: Token.Xml.Part) = {

        val patched = tok.value
          .replaceAllLiterally("{{", "{")
          .replaceAllLiterally("}}", "}")
        ctx.replaceToken(tok, patched) +
          ctx.lint(singleBracesEscape.at(tok.pos))
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
          if (part.contains("{{") || part.contains("}}"))
            patch += patchEscapedBraces(tok)
          patch

      }.asPatch
    }

    /* add `import scala.xml.quote._` */
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
      case xml @ Term.Xml(parts, _) =>
        val tripleQuoted = isMultiLine(xml) || parts.exists(
          containsEscapeSequence)
        patchXml(xml, tripleQuoted).atomic
    }.asPatch

    // NB if all patches are not yet escaped here
    // it can be empty and only have the import
    if (patch.nonEmpty) patch + importXmlQuote
    else patch
  }
}
