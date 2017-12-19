package scalafix.internal.sbt

import java.nio.file.Paths
import org.scalatest.FunSuite
import sbt.complete.Parser
import sbt.complete.Completion

import org.eclipse.jgit.lib.AbbreviatedObjectId

class ScalafixCompletionsTest extends FunSuite {
  val cwd = Paths.get("").toAbsolutePath
  val parser = ScalafixCompletions.parser(cwd)
  val space = " "

  def check(name: String)(assertCompletions: List[Completion] => Unit): Unit = {
    val option =
      if (name == "all") ""
      else name

    test(name) {
      val completions = Parser.completions(parser, " " + option, 0).get
      assertCompletions(completions.toList.sortBy(_.display))
    }
  }

  def check2(name: String)(
      assertCompletions: (List[String], List[String]) => Unit): Unit = {
    check(name) { completions =>
      val appends = completions.map(_.append)
      val displays = completions.map(_.display)

      assertCompletions(appends, displays)
    }
  }

  def isSha1(in: String): Boolean =
    AbbreviatedObjectId.isId(in)

  check("all") { completions =>
    val obtained = completions.map(_.display)
    val expected = List(
      "",
      "--bash",
      "--classpath",
      "--classpath-auto-roots",
      "--config",
      "--config-str",
      "--diff",
      "--diff-base",
      "--exclude",
      "--help",
      "--no-strict-semanticdb",
      "--no-sys-exit",
      "--non-interactive",
      "--out-from",
      "--out-to",
      "--quiet-parse-errors",
      "--rules",
      "--single-thread",
      "--sourceroot",
      "--stdout",
      "--test",
      "--tool-classpath",
      "--usage",
      "--verbose",
      "--version",
      "--zsh"
    )

    assert(obtained == expected)
  }
  check2("--classpath scalafix-co") { (appends, displays) =>
    assert(displays.contains("scalafix-core"))
    assert(appends.contains("re"))
  }
  check2("--classpath scalafix-core") { (appends, displays) =>
    assert(displays.contains(":"))
    assert(appends.contains(":"))
  }
  check2("--classpath scalafix-core:") { (appends, displays) =>
    assert(displays.contains("scalafix-cli"))
    assert(appends.contains("scalafix-cli"))
  }
  check2("--classpath-auto-roots" + space) { (appends, displays) =>
    assert(displays.contains("scalafix-cli"))
    assert(appends.contains("scalafix-cli"))
  }
  check2("--config" + space) { (appends, displays) =>
    assert(displays.contains(".scalafmt.conf"))
  }
  check("""--config-str="ExplicitResult"""") { completions =>
    assert(completions.isEmpty)
  }
  check("--diff-base" + space) { completions =>
    val displays = completions.map(_.display)

    // branches
    assert(displays.contains("master"))

    // tag
    assert(displays.contains("v0.5.0"))

    // last 20 commits
    completions.reverse.take(20).foreach { completion =>
      assert(isSha1(completion.append))
      assert(completion.display.contains(completion.append))
      assert(completion.display.endsWith("ago)"))
    }
  }
  check2("--exclude" + space) { (appends, displays) =>
    assert(displays.contains("scalafix-cli"))
    assert(appends.contains("scalafix-cli"))
  }
  check2("--out-from" + space) { (appends, displays) =>
    assert(displays.contains("scalafix-cli"))
    assert(appends.contains("scalafix-cli"))
  }
  check2("--out-to" + space) { (appends, displays) =>
    assert(displays.contains("scalafix-cli"))
    assert(appends.contains("scalafix-cli"))
  }
  check2("--rules" + space) { (_, displays) =>
    // built-in
    assert(displays.contains("NoInfer"))

    // protocols
    assert(displays.contains("file:"))
    assert(displays.contains("github:"))
    assert(displays.contains("http:"))
    assert(displays.contains("https:"))
    assert(displays.contains("replace:"))
    assert(displays.contains("scala:"))
  }
  check2("--rules file:scalafix/../") { (appends, displays) =>
    // resolve parent directories
    assert(appends.contains("scalafix-cli"))
    assert(displays.contains("scalafix/../scalafix-cli"))
  }
  check2("--sourceroot" + space) { (appends, displays) =>
    assert(displays.contains("scalafix-cli"))
    assert(appends.contains("scalafix-cli"))
  }
  check2("--tool-classpath scalafix-co") { (appends, displays) =>
    assert(displays.contains("scalafix-core"))
    assert(appends.contains("re"))
  }
  check2("--tool-classpath scalafix-core") { (appends, displays) =>
    assert(displays.contains(":"))
    assert(appends.contains(":"))
  }
  check2("--tool-classpath scalafix-core:") { (appends, displays) =>
    assert(displays.contains("scalafix-cli"))
    assert(appends.contains("scalafix-cli"))
  }
}
