package scalafix.internal.sbt

import java.io.File
import sbt.complete.DefaultParsers
import sbt.complete.DefaultParsers._
import sbt.complete.FileExamples
import sbt.complete.Parser

object ScalafixCompletions {
  private val names = ScalafixRewriteNames.all

  private def uri(protocol: String) =
    token(protocol + ":") ~> NotQuoted.map(x => s"$protocol:$x")

  private def fileRewrite(base: File): Parser[String] =
    token("file:") ~>
      StringBasic
        .examples(new FileExamples(base))
        .map(f => s"file:${new File(base, f).getAbsolutePath}")

  private val namedRewrite: Parser[String] =
    names.map(literal).reduceLeft(_ | _)

  def parser(base: File): Parser[Seq[String]] = {
    val all =
      namedRewrite |
        fileRewrite(base) |
        uri("github") |
        uri("replace") |
        uri("http") |
        uri("https") |
        uri("scala")
    (token(Space) ~> token(all)).* <~ SpaceClass.*
  }
}
