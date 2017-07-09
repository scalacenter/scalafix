package scalafix
package rewrite

import scala.meta._
import scalafix.syntax._
import scalafix.util.{Whitespace => _, _}
import scala.meta.contrib._
import scala.meta.tokens.Token._

//TODO(gabro): move the helpers somewhere else
import NoExtendsAppSyntax._

case class NoExtendsApp(mirror: Mirror) extends SemanticRewrite(mirror) {
  override def rewrite(ctx: RewriteCtx): Patch = {
    def wrapBodyInMain(template: Template) =
      ctx
        .templateBodyTokens(template) match {
        case None =>
          ctx.reporter.warn(
            "Missing or empty body for object that extends scala.App. See http://dotty.epfl.ch/docs/reference/dropped/delayed-init.html for possible workarounds.",
            template.pos
          )
          Patch.empty
        case Some(body) =>
          val open =
            ctx.addLeft(
              body.head,
              s"\n  def main(args: Array[String]): Unit = {")
          val indentBody = ctx.indent(body)
          // this handles bodies on a single line like:
          //   object Main extends App { println(args(0)) }
          val closingSpaces = if (indentBody.isDefined) " " * 2 else ""
          val close = ctx.addRight(body.last, s"$closingSpaces}\n")
          open + indentBody.asPatch + close
      }

    ctx.tree.collect {
      case t: Defn.Object =>
        ctx.removeParentFromTemplate(Symbol("_root_.scala.App."), t.templ) +
          wrapBodyInMain(t.templ)
    }.asPatch
  }
}

object NoExtendsAppSyntax {
  implicit class XtensionRewriteCtx(ctx: RewriteCtx) {

    def templateBodyTokens(template: Template): Option[Tokens] = {
      val tokens = template.tokens
      val maybeTokens = for {
        close <- tokens.lastOption
        if close.is[RightBrace]
        open <- ctx.matching.open(close.asInstanceOf[RightBrace])
      } yield
        tokens
          .dropWhile(_.pos.start.offset <= open.pos.start.offset)
          .dropRight(1)
      maybeTokens.filterNot(_.isEmpty)
    }

    def indent(tokens: Tokens, numberOfSpaces: Int = 2): Option[Patch] =
      tokens.dropRightWhile(t => !t.is[Newline]).lastOption.map { lastNewLine =>
        tokens.collect {
          case nl @ Newline() if nl != lastNewLine =>
            ctx.addRight(nl, " " * numberOfSpaces)
        }.asPatch
      }

    private[scalafix] def removeTokensBetween(
        from: Token,
        to: Token,
        removeLeadingWhitespace: Boolean = true): Patch = {
      val toRemove = ctx.tokenList
        .slice(
          ctx.tokenList.prev(from),
          ctx.tokenList
            .next(to) // apply next twice to include the trailing space
        )
      val leadingToRemove =
        if (removeLeadingWhitespace)
          ctx.tokenList.leading(from).takeWhile(_.is[Whitespace])
        else
          Nil

      (toRemove ++ leadingToRemove)
        .map(ctx.removeToken)
        .asPatch
    }

    def removeParentFromTemplate(normalized: Symbol, template: Template)(
        implicit m: Mirror): Patch = {
      val maybePatch = for {
        treeToRemove <- template.parents
          .collect { case c: Ctor.Ref => c }
          .find(_.symbolOpt.map(_.normalized) == Some(normalized))
        nameToRemove <- treeToRemove.tokens.headOption
        leadingExtendsOrWithToken <- ctx.tokenList
          .leading(nameToRemove)
          .find(t => t.is[KwExtends] || t.is[KwWith])
      } yield
        (template.parents.length, leadingExtendsOrWithToken) match {
          // 1) object Foo extends ToRemove { ... }
          // or
          // 2) object Foo extends Something with ToRemove { ... }
          case (1, _) | (_, KwWith()) =>
            ctx.removeTokensBetween(leadingExtendsOrWithToken, nameToRemove)

          // 3) object Foo extends ToRemove with Something
          case (_, KwExtends()) =>
            (for {
              trailingWith <- ctx.tokenList
                .trailing(nameToRemove)
                .find(t => t.is[KwWith])
            } yield ctx.removeTokensBetween(nameToRemove, trailingWith)).asPatch
        }
      maybePatch.asPatch
    }
  }
}
