package scalafix.internal.sbt

import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import sbt.complete.DefaultParsers
import sbt.complete.DefaultParsers._
import sbt.complete.FileExamples
import sbt.complete.Parser

object ScalafixCompletions {
  private val names = ScalafixRuleNames.all

  private def toAbsolutePath(path: Path, cwd: Path): Path = {
    if (path.isAbsolute) path
    else cwd.resolve(path)
  }.normalize()

  // Extend FileExamples to tab complete when the prefix is an absolute path or `..`
  private class AbsolutePathExamples(cwd: Path, prefix: String = "")
      extends FileExamples(cwd.toFile, prefix) {
    override def withAddedPrefix(addedPrefix: String): FileExamples = {
      val nextPrefix =
        if (addedPrefix.startsWith(".")) addedPrefix
        else prefix + addedPrefix
      val (b, p) = AbsolutePathCompleter.mkBase(nextPrefix, cwd)
      new AbsolutePathExamples(b, p)
    }
  }
  private object AbsolutePathCompleter {
    def mkBase(prefix: String, fallback: Path): (Path, String) = {
      val path = toAbsolutePath(Paths.get(prefix), fallback)
      if (prefix.endsWith(File.separator)) path -> ""
      else path.getParent -> path.getFileName.toString
    }
  }

  private def fileRule(cwd: Path): Parser[String] =
    token("file:") ~>
      StringBasic
        .examples(new AbsolutePathExamples(cwd))
        .map { f =>
          val path = toAbsolutePath(Paths.get(f), cwd).toString
          "file:" + path
        }

  private def uri(protocol: String) =
    token(protocol + ":") ~> NotQuoted.map(x => s"$protocol:$x")

  private val namedRule: Parser[String] =
    names.map(literal).reduceLeft(_ | _)

  def parser(cwd: Path): Parser[Seq[String]] = {
    val all =
      namedRule |
        fileRule(cwd) |
        uri("github") |
        uri("replace") |
        uri("http") |
        uri("https") |
        uri("scala")
    (token(Space) ~> token(all)).* <~ SpaceClass.*
  }
}
