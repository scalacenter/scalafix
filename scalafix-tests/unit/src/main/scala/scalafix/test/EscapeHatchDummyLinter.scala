package scalafix.test

import scalafix.LintCategory
import scalafix.LintMessage
import scalafix.Rule
import scalafix.RuleCtx

import scala.meta._

object EscapeHatchDummyLinter extends Rule("EscapeHatchDummyLinter") {
  val error = LintCategory.error("Dummy!")

  override def check(ctx: RuleCtx): Seq[LintMessage] = {
    ctx.tree.collect {
      case tree @ Name(name) if name.contains("Dummy") => error.at(tree.pos)
    }
  }
}

object EscapeHatchDummyLinterA extends Rule("EscapeHatchDummyLinterA") {
  val error = LintCategory.error("Bam A!")

  override def check(ctx: RuleCtx): Seq[LintMessage] = {
    ctx.tree.collect {
      case tree @ q"A" => error.at(tree.pos)
    }
  }
}

object EscapeHatchDummyLinterB extends Rule("EscapeHatchDummyLinterB") {
  val error = LintCategory.error("Bam B!")

  override def check(ctx: RuleCtx): Seq[LintMessage] = {
    ctx.tree.collect {
      case tree @ q"B" => error.at(tree.pos)
    }
  }
}
