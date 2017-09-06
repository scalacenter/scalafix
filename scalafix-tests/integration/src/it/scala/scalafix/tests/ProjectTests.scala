package scalafix.tests

import scalafix.SemanticCtx
import scalafix.internal.rule.ExplicitResultTypes
import scalafix.internal.rule.ProcedureSyntax
import scalafix.internal.rule.RemoveUnusedImports
import scalafix.rule.ProcedureSyntax
import scalafix.rule.RemoveUnusedImports
import scalafix.rule.ScalafixRewrites

class Slick
    extends IntegrationPropertyTest(
      ItTest(
        name = "slick",
        repo = "https://github.com/slick/slick.git",
        rules = Seq(
          ProcedureSyntax.name,
          ExplicitResultTypes(ScalafixRewrites.emptyDatabase).name,
          RemoveUnusedImports(ScalafixRewrites.emptyDatabase).name
        ),
        hash = "bd3c24be419ff2791c123067668c81e7de858915",
        addCoursier = false,
        commands =
          Command.enableWarnUnusedImports +:
            Command.default
      ),
      skip = false
    )
