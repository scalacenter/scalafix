package scalafix.internal.rule

import scala.meta._
import metaconfig.{Conf, Configured}
import scalafix.rule.{Rule, RuleCtx}
import scalafix.lint.LintMessage
import scalafix.lint.LintCategory
import scalafix.internal.config.{DisableSyntaxConfig, Keyword}

final case class DisableSyntax(
    config: DisableSyntaxConfig = DisableSyntaxConfig())
    extends Rule("DisableSyntax")
    with Product {

  override def description: String =
    "Linter that reports an error on a configurable set of keywords and syntax."

  override def init(config: Conf): Configured[Rule] =
    config
      .getOrElse("disableSyntax", "DisableSyntax")(DisableSyntaxConfig.default)
      .map(DisableSyntax(_))

  override def check(ctx: RuleCtx): Seq[LintMessage] = {
    def pos(offset: Int): Position =
      Position.Range(ctx.input, offset, offset)
    val regexLintMessages = Seq.newBuilder[LintMessage]
    config.regex.foreach { regex =>
      val matcher = regex.value.matcher(ctx.input.chars)
      val pattern = regex.value.pattern
      val message = regex.message.getOrElse(s"$pattern is disabled")
      while (matcher.find()) {
        regexLintMessages +=
          errorCategory
            .copy(id = regex.id.getOrElse(pattern))
            .at(message, pos(matcher.start))
      }
    }
    val tokensLintMessage =
      ctx.tree.tokens.collect {
        case token @ Keyword(keyword) if config.isDisabled(keyword) =>
          errorCategory
            .copy(id = s"keywords.$keyword")
            .at(s"$keyword is disabled", token.pos)
        case token @ Token.Semicolon() if config.noSemicolons =>
          error("noSemicolons", token)
        case token @ Token.Tab() if config.noTabs =>
          error("noTabs", token)
        case token @ Token.Xml.Start() if config.noXml =>
          error("noXml", token)
      }.toSeq
    val treeLintMessages =
      ctx.tree.collect {
        case t @ mod"+" if config.noCovariantTypes =>
          errorCategory
            .copy(id = "covariant")
            .at(
              "Covariant types could lead to error-prone situations.",
              t.pos
            )
        case t @ mod"-" if config.noContravariantTypes =>
          errorCategory
            .copy(id = "contravariant")
            .at(
              "Contravariant types could lead to error-prone situations.",
              t.pos
            )
      }

    treeLintMessages ++ tokensLintMessage ++ regexLintMessages.result()
  }

  private val errorCategory: LintCategory =
    LintCategory.error(
      "Some constructs are unsafe to use and should be avoided")

  private def error(keyword: String, token: Token): LintMessage =
    errorCategory.copy(id = keyword).at(s"$keyword is disabled", token.pos)
}
