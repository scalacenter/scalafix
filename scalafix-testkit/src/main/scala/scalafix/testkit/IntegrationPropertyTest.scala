package scalafix.tests

import scalafix.Versions
import ammonite.ops._
import org.scalameta.logger
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
    write.append(
      t.workingPath / ".jvmopts",
      """-Xmx8g
        |-Xms1g
        |-Xss16m
        |""".stripMargin
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
      logger.elem(s"Running $id")
      val sbt = if (sys.env.contains("DRONE")) "/usr/bin/sbt" else "sbt"
      logger.elem(sbt)
      val args = Seq(
        sbt,
        "++" + Versions.scala212
      ) ++ cmds.map(_.toString)
      failAfter(maxTime) {
        import sys.process._
        val status = Process(args, cwd = t.workingPath.toIO).!
        assert(status == 0)
      }
      logger.elem(s"Completed $id")
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
