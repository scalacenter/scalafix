package scalafix.internal.sbt

import org.scalatest.FunSuite
import sbt.complete.Parser
import sbt.complete.Completion
import sbt.scalafixsbt.JLineAccess

import org.eclipse.jgit.lib.AbbreviatedObjectId
import scalafix.internal.tests.utils.{Fs, Git}
import scalafix.internal.tests.utils.SkipWindows
import org.scalatest.Tag

trait MockJLineAccess extends JLineAccess {
  override def terminalWidth: Int = 60
}
object ScalafixCompletions
    extends ScalafixCompletionsComponent
    with MockJLineAccess

class SbtCompletionsSuite extends FunSuite {
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

  def check(name: String, testTags: Tag*)(
      assertCompletions: List[Completion] => Unit): Unit = {
    val option =
      if (name == "all") ""
      else name

    test(name, testTags: _*) {
      val completions = Parser.completions(parser, " " + option, 0).get
      assertCompletions(completions.toList.sortBy(_.display))
    }
  }

  def check2(name: String, testTags: Tag*)(
      assertCompletions: (List[String], List[String]) => Unit): Unit = {
    check(name, testTags: _*) { completions =>
      val appends = completions.map(_.append)
      val displays = completions.map(_.display)

      assertCompletions(appends, displays)
    }
  }

  def isSha1(in: String): Boolean =
    AbbreviatedObjectId.isId(in)

  def compat(name: String, testTags: Tag*)(
      assertCompletions: List[Completion] => Unit): Unit = {
    val option =
      if (name == "all") ""
      else name

    test(s"compat $name", testTags: _*) {
      val parser = ScalafixCompletions.parser(
        fs.workingDirectory.toAbsolutePath,
        compat = true)
      val completions = Parser.completions(parser, " " + option, 0).get
      assertCompletions(completions.toList.sortBy(_.display))
    }
  }

  compat("all") { completions =>
    assert(completions.exists(_.display.startsWith("file:")))
  }
  compat("RemoveUnusedImports Remo") { completions =>
    val termWidth = ScalafixCompletions.terminalWidth
    val ruleMap = ScalafixRuleNames.all.toMap
    val obtained = completions.toList.map(_.display).toSet
    val expected = Set(
      s"RemoveUnusedImports -- ${ruleMap.getOrElse("RemoveUnusedImports", "")}"
        .take(termWidth),
      s"RemoveUnusedTerms   -- ${ruleMap.getOrElse("RemoveUnusedTerms", "")}"
        .take(termWidth),
      s"RemoveXmlLiterals   -- ${ruleMap.getOrElse("RemoveXmlLiterals", "")}"
        .take(termWidth)
    )
    assert((expected -- obtained).isEmpty)
  }
  check("all", SkipWindows) { completions =>
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
  check2("--classpath ba", SkipWindows) { (appends, displays) =>
    assert(displays.contains("bar"))
    assert(appends.contains("r"))
  }
  check2("--classpath bar", SkipWindows) { (appends, displays) =>
    assert(displays.contains(":"))
    assert(appends.contains(":"))
  }
  check2("--classpath bar:", SkipWindows) { (appends, displays) =>
    assert(displays.contains("foo"))
    assert(appends.contains("foo"))
  }
  check2("--classpath-auto-roots" + space, SkipWindows) { (appends, displays) =>
    assert(displays.contains("foo"))
    assert(appends.contains("foo"))
  }
  check2("--config" + space, SkipWindows) { (appends, displays) =>
    assert(displays.contains("my.conf"))
  }
  check("--diff-base" + space, SkipWindows) { completions =>
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
  check2("--exclude" + space, SkipWindows) { (appends, displays) =>
    assert(displays.contains("foo"))
    assert(appends.contains("foo"))
  }
  check2("--out-from" + space, SkipWindows) { (appends, displays) =>
    assert(displays.contains("foo"))
    assert(appends.contains("foo"))
  }
  check2("--out-to" + space, SkipWindows) { (appends, displays) =>
    assert(displays.contains("foo"))
    assert(appends.contains("foo"))
  }
  check2("--rules" + space, SkipWindows) { (_, displays) =>
    // built-in
    assert(displays.map(_.startsWith("NoInfer")).reduceLeft(_ || _))

    // protocols
    assert(displays.contains("file:"))
    assert(displays.contains("github:"))
    assert(displays.contains("http:"))
    assert(displays.contains("https:"))
    assert(displays.contains("replace:"))
    assert(displays.contains("scala:"))
  }
  check2("--rules file:bar/../", SkipWindows) { (appends, displays) =>
    // resolve parent directories
    assert(appends.contains("foo"))
    assert(displays.contains("bar/../foo"))
  }
  check2("--sourceroot" + space, SkipWindows) { (appends, displays) =>
    assert(displays.contains("bar"))
    assert(appends.contains("bar"))
  }
  check2("--tool-classpath ba", SkipWindows) { (appends, displays) =>
    assert(displays.contains("bar"))
    assert(appends.contains("r"))
  }
  check2("--tool-classpath bar", SkipWindows) { (appends, displays) =>
    assert(displays.contains(":"))
    assert(appends.contains(":"))
  }
  check2("--tool-classpath bar:", SkipWindows) { (appends, displays) =>
    assert(displays.contains("buzz"))
    assert(appends.contains("buzz"))
  }
}
