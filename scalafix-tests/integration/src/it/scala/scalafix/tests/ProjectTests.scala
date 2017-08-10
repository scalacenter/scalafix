package scalafix.tests

import scalafix.SemanticCtx
import scalafix.rewrite.ExplicitReturnTypes
import scalafix.rewrite.ProcedureSyntax
import scalafix.rewrite.RemoveUnusedImports
import scalafix.rewrite.ScalafixRewrites

class Slick
    extends IntegrationPropertyTest(
      ItTest(
        name = "slick",
        repo = "https://github.com/slick/slick.git",
        rewrites = Seq(
          ProcedureSyntax.name,
          ExplicitReturnTypes(ScalafixRewrites.emptyDatabase).name,
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
