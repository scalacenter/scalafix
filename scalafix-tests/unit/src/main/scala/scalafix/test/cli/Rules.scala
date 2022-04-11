package scalafix.tests.cli

import metaconfig._
import scalafix.v1
import scalafix.v1._

class CrashingRule extends SyntacticRule("CrashingRule") {
  override def fix(implicit doc: SyntacticDocument): _root_.scalafix.v1.Patch =
    throw new MissingSymbolException(Symbol("local63"))
}

class NoOpRule extends SyntacticRule("NoOpRule") {
  override def fix(implicit doc: SyntacticDocument): _root_.scalafix.v1.Patch =
    Patch.empty
}

class DeprecatedName
    extends SyntacticRule(
      RuleName("DeprecatedName").withDeprecatedName(
        "OldDeprecatedName",
        "Use DeprecatedName instead",
        "1.0"
      )
    ) {
  override def fix(implicit doc: SyntacticDocument): _root_.scalafix.v1.Patch =
    Patch.empty
}

class Scala2_9 extends SyntacticRule("Scala2_9") {
  override def withConfiguration(config: Configuration): Configured[Rule] =
    if (!config.scalaVersion.startsWith("2.9")) {
      Configured.error("scalaVersion must start with 2.9")
    } else if (!config.scalacOptions.contains("-Ysource:2.9")) {
      Configured.error("scalacOptions must contain -Ysource:2.9")
    } else {
      Configured.ok(this)
    }
}

class AvailableRule
    extends v1.SemanticRule(
      v1.RuleName("AvailableRule")
        .withDeprecatedName("DeprecatedAvailableRule", "", "")
    )
