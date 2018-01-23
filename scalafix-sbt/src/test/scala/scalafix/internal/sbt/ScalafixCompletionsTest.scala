package scalafix.internal.sbt

import org.scalatest.FunSuite
import sbt.complete.Parser
import sbt.complete.Completion

import org.eclipse.jgit.lib.AbbreviatedObjectId
import scalafix.internal.tests.utils.{Fs, Git}

class ScalafixCompletionsTest extends FunSuite {
  val fs = new Fs()
  val git = new Git(fs.workingDirectory)
  fs.mkdir("foo")
  fs.add("foo/f.scala", "object F")
  fs.mkdir("bar")
  fs.add("bar/b.scala", "object B")
  fs.mkdir("buzz")
  fs.add("my.conf", "conf.file = []")
  git.commit()
  // create at least 20 commits for diff-base log
  (1 to 20).foreach { i =>
    fs.add(s"f$i", s"f$i")
    git.commit()
  }
  git.tag("v0.1.0")

  val parser = ScalafixCompletions.parser(
    fs.workingDirectory.toAbsolutePath,
    compat = false)
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

  test("compat") {
    val parser = ScalafixCompletions.parser(
      fs.workingDirectory.toAbsolutePath,
      compat = true)
    val completions = Parser.completions(parser, " ", 0).get
    val obtained = completions.toList.sortBy(_.display)
    assert(obtained.exists(_.display.startsWith("file:")))
  }

  test("compat multiple") {
    val parser = ScalafixCompletions.parser(
      fs.workingDirectory.toAbsolutePath,
      compat = true)
    val completions =
      Parser.completions(parser, " RemoveUnusedImports Remo", 0).get
    val obtained = completions.toList.map(_.display).toSet
    val expected = Set(
      "RemoveUnusedImports",
      "RemoveUnusedTerms",
      "RemoveXmlLiterals"
    )
    assert((expected -- obtained).isEmpty)
  }

  check("all") { completions =>
    val obtained = completions.map(_.display).toSet
    val expected = Set(
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

    assert((expected -- obtained).isEmpty)
  }
  check2("--classpath ba") { (appends, displays) =>
    assert(displays.contains("bar"))
    assert(appends.contains("r"))
  }
  check2("--classpath bar") { (appends, displays) =>
    assert(displays.contains(":"))
    assert(appends.contains(":"))
  }
  check2("--classpath bar:") { (appends, displays) =>
    assert(displays.contains("foo"))
    assert(appends.contains("foo"))
  }
  check2("--classpath-auto-roots" + space) { (appends, displays) =>
    assert(displays.contains("foo"))
    assert(appends.contains("foo"))
  }
  check2("--config" + space) { (appends, displays) =>
    assert(displays.contains("my.conf"))
  }
  check("--diff-base" + space) { completions =>
    val displays = completions.map(_.display)

    // branches
    assert(displays.contains("master"))

    // tag
    assert(displays.contains("v0.1.0"))

    // last 20 commits
    completions.reverse.take(20).foreach { completion =>
      assert(isSha1(completion.append))
      assert(completion.display.contains(completion.append))
      assert(completion.display.endsWith("ago)"))
    }
  }
  check2("--exclude" + space) { (appends, displays) =>
    assert(displays.contains("foo"))
    assert(appends.contains("foo"))
  }
  check2("--out-from" + space) { (appends, displays) =>
    assert(displays.contains("foo"))
    assert(appends.contains("foo"))
  }
  check2("--out-to" + space) { (appends, displays) =>
    assert(displays.contains("foo"))
    assert(appends.contains("foo"))
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
  check2("--rules file:bar/../") { (appends, displays) =>
    // resolve parent directories
    assert(appends.contains("foo"))
    assert(displays.contains("bar/../foo"))
  }
  check2("--sourceroot" + space) { (appends, displays) =>
    assert(displays.contains("bar"))
    assert(appends.contains("bar"))
  }
  check2("--tool-classpath ba") { (appends, displays) =>
    assert(displays.contains("bar"))
    assert(appends.contains("r"))
  }
  check2("--tool-classpath bar") { (appends, displays) =>
    assert(displays.contains(":"))
    assert(appends.contains(":"))
  }
  check2("--tool-classpath bar:") { (appends, displays) =>
    assert(displays.contains("buzz"))
    assert(appends.contains("buzz"))
  }
}
