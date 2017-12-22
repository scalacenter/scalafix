package scalafix.internal.rule

import scalafix.rule.RuleCtx
import scalafix.{LintMessage, SemanticRule, SemanticdbIndex}
import scala.meta._
import scalafix.lint.LintCategory

final case class DisableVariantTypes(index: SemanticdbIndex)
    extends SemanticRule(index, "DisableVariantTypes") {

  private lazy val errorCategory: LintCategory =
    LintCategory.error(
      """Covariant and Contravariant type parameters are disabled""".stripMargin
    )

  override def check(ctx: RuleCtx): Seq[LintMessage] = {
    ctx.tree.collect {
      case t @ Type.Param(mods, _, _, _, _, _)
          if mods.exists(_.is[Mod.Covariant]) =>
        errorCategory.copy(id = "Covariant").at(t.pos)
      case t @ Type.Param(mods, _, _, _, _, _)
          if mods.exists(_.is[Mod.Contravariant]) =>
        errorCategory.copy(id = "Contravariant").at(t.pos)
    }
  }
}
