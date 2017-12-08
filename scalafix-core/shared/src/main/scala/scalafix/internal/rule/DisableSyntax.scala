package scalafix.internal.rule

import scala.meta._
import metaconfig.{Conf, Configured}
import scalafix.rule.{Rule, RuleCtx}
import scalafix.lint.LintMessage
import scalafix.lint.LintCategory
import scalafix.util.SymbolMatcher
import scalafix.internal.config.{DisableSyntaxConfig, Keyword}
import scalafix.syntax._

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
    ctx.tree.tokens.collect {
      case token @ Keyword(keyword) if config.isDisabled(keyword) =>
        errorCategory.copy(id = s"keywords.$keyword").at(s"$keyword is disabled", token.pos)
      case token @ Token.Semicolon() if config.noSemicolons =>
        error("noSemicolons", token)
      case token @ Token.Tab() if config.noTabs =>
        error("noTabs", token)
      case token @ Token.Xml.Start() if config.noXml =>
        error("noXml", token)
    }.toSeq
  }

  private val errorCategory: LintCategory =
    LintCategory.error(
      "Some constructs are unsafe to use and should be avoided")

  private def error(keyword: String, token: Token): LintMessage =
    errorCategory.copy(id = keyword).at(s"$keyword is disabled", token.pos)
}
