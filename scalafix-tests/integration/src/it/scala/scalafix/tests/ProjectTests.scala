package scalafix.tests

import scalafix.rewrite.ProcedureSyntax

class Slick
    extends IntegrationPropertyTest(
      ItTest(
        name = "slick",
        repo = "https://github.com/slick/slick.git",
        rewrites = Seq(ProcedureSyntax.toString, "ExplicitImplicit"),
        hash = "bd3c24be419ff2791c123067668c81e7de858915",
        addCoursier = false
      ),
      skip = false
    )
