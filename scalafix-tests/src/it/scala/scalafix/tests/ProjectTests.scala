package scalafix.tests

import scalafix.rewrite.ProcedureSyntax
import scalafix.rewrite.ScalaJsRewrites

class Akka
    extends IntegrationPropertyTest(
      ItTest(
        name = "akka",
        repo = "https://github.com/akka/akka.git",
        hash = "3936883e9ae9ef0f7a3b0eaf2ccb4c0878fcb145",
        rewrites = Seq()
      ),
      skip = true
    )

class Circe
    extends IntegrationPropertyTest(
      ItTest(
        name = "circe",
        repo = "https://github.com/circe/circe.git",
        hash = "717e1d7d5d146cbd0455770771261e334f419b14",
        rewrites = Seq()
      ),
      skip = true
    )

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

class Scalaz
    extends IntegrationPropertyTest(
      ItTest(
        name = "scalaz",
        repo = "https://github.com/scalaz/scalaz.git",
        hash = "cba156fb2f1f178dbaa32cbca21e95f8199d2f91"
      ),
      skip = true // kind-projector causes problems.
    )

class Cats
    extends IntegrationPropertyTest(
      ItTest(
        name = "cats",
        repo = "https://github.com/typelevel/cats.git",
        config = ItTest.catsImportConfig,
        hash = "31080daf3fd8c6ddd80ceee966a8b3eada578198"
      ),
      skip = true
    )

class Monix
    extends IntegrationPropertyTest(
      ItTest(
        name = "monix",
        repo = "https://github.com/monix/monix.git",
        hash = "45c15b5989685668f5ad7ec886af6b74b881a7b4"
      ),
      // monix fails on reporter info messages and scala.meta has a parser bug.
      // Pipe.scala:32: error: identifier expected but ] found
      // [error] extends ObservableLike[O, ({type ?[+?] = Pipe[I, ?]})#?] {
      skip = true
    )

class ScalaJs
    extends IntegrationPropertyTest(
      ItTest(
        name = "Scala.js",
        repo = "https://github.com/scala-js/scala-js.git",
        hash = "8917b5a9bd8fb2175a112fc15c761050eeb4099f",
        commands = Seq(
          Command("set scalafixEnabled in Global := true"),
          Command("compiler/test:compile"),
          Command("examples/test:compile")
        )
      ),
      skip = true // GenJsCode is hard: import renames + dependent types
    )

class ScalacheckShapeless
    extends IntegrationPropertyTest(
      ItTest(
        name = "scalacheck-shapeless",
        repo = "https://github.com/alexarchambault/scalacheck-shapeless.git",
        hash = "bb25ecee23c42148f66d9b27920a89ba5cc189d2",
        addCoursier = false
      ),
      skip = true // coursier can't resolve locally published snapshot on ci, sbt.ivy.home is not read.
    )

class ScalafixIntegrationTest
    extends IntegrationPropertyTest(
      ItTest(
        name = "scalafix",
        repo = "https://github.com/scalacenter/scalafix.git",
        hash = "69069ff8d028f9fc553fac62c28b3d4eb6707bcb",
        rewrites = Nil,
        commands = Seq(
          Command.clean,
          Command.enableScalafix,
          Command("scalafix-nsc/test:compile"),
          Command.disableScalafix
        ),
        addCoursier = false
      ),
      skip = true
    )

class ScalajsBootstrap
    extends IntegrationPropertyTest(
      ItTest(
        name = "scalajs-bootstrap",
        repo = "https://github.com/Karasiq/scalajs-bootstrap.git",
        hash = "1cf125a8f78951df9a1a274f19b81221e55ad4bd",
        rewrites = List("DemandJSGlobal"),
        commands = Seq(
          Command("scalafix")
        )
      ),
      skip = true
    )

class ScalajsSri
    extends IntegrationPropertyTest(
      ItTest(
        name = "sri",
        repo = "https://github.com/chandu0101/sri.git",
        hash = "2526f0574f7ef8088f209ff50d38f72c458e0a62",
        rewrites = List("DemandJSGlobal"),
        config = "imports.organize = false",
        commands = Seq(
          Command("scalafix")
        )
      ),
      skip = true
    )
