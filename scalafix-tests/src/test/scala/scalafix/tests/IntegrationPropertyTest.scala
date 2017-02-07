package scalafix.tests

import scalafix.rewrite.ExplicitImplicit
import scalafix.util.FileOps
import scalafix.util.logger

import ammonite.ops._
import org.scalatest.FunSuite
import org.scalatest.concurrent.TimeLimits
import org.scalatest.time.Minutes
import org.scalatest.time.Span

// Clones the repo, adds scalafix as a plugin and tests that the
// following commands success:
// 1. test:compile
// 2. scalafix
// 3. test:compile
abstract class IntegrationPropertyTest(t: ItTest, skip: Boolean = false)
    extends FunSuite
    with TimeLimits {

  private val isCi = sys.props.contains("CI")
  private val maxTime = Span(20, Minutes) // just in case.

  val hardClean = true

  // Clones/cleans/checkouts
  def setup(t: ItTest): Unit = {
    t.parentDir.mkdirs()
    if (!t.workingPath.toIO.exists()) {
      %%("git", "clone", t.repo)(ItTest.root)
    }
    if (hardClean) {
      %%("git", "clean", "-fd")(t.workingPath)
      %%("git", "checkout", t.hash)(t.workingPath)
    } else {
      %%("git", "checkout", "--", ".")(t.workingPath)
    }
    %%("git", "reset", "--hard", t.hash)(t.workingPath)
    // TODO(olafur) better solution.
    rm(t.workingPath / ".jvmopts") // avoid jvm "Conflicting collector combinations"
    write.over(
      t.workingPath / "project" / "build.properties",
      "sbt.version=0.13.13\n"
    )
    if (t.addCoursier) {
      write.over(
        t.workingPath / "project" / "plugin-coursier.sbt",
        """addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.0-M15")
        """.stripMargin
      )
    }
    write.over(
      t.workingPath / ".scalafix.conf",
      s"""rewrites = [${t.rewrites.mkString(", ")}]
         |fatalWarnings = true
         |${t.config}
         |""".stripMargin
    )
    write.append(
      t.workingPath / "project" / "plugins.sbt",
      s"""
         |addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "${scalafix.Versions.version}")
         |""".stripMargin
    )
  }

  case class FailOk(cmd: String, msg: String) extends Exception(msg)
  def check(): Unit = {

    def assertDiffIsNonEmpty(): Unit = {
      %%("git", "diff", "--name-only")(t.workingPath).out.lines
        .exists(_.endsWith(".scala"))
    }
    def sbt(cmds: Seq[Command]): Unit = {
      val cmd = cmds.mkString("; ", "; ", "")
      val id = s"${t.name}/$cmd"
      logger.info(s"Running $id")
      val args = Seq(
          "sbt",
          "++2.11.8"
        ) ++ cmds.map(_.toString)
      failAfter(maxTime) {
        import sys.process._
        val status = Process(args, cwd = t.workingPath.toIO).!
        assert(status == 0)
      }
      logger.info(s"Completed $id")
    }
    val testFun: () => Any = { () =>
      setup(t)
      sbt(t.commands)
      assertDiffIsNonEmpty()
    }

    if (skip) ignore(t.name)(testFun())
    else test(t.name)(testFun())
  }
  check()
}

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
        rewrites = Seq(),
        testPatch = true,
        hash = "bd3c24be419ff2791c123067668c81e7de858915"
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
