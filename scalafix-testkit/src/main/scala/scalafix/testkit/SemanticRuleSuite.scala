package scalafix.testkit

import scala.meta._

import scalafix.internal.reflect.RuleCompilerClasspath
import scalafix.internal.testkit.EndOfLineAssertExtractor
import scalafix.internal.testkit.MultiLineAssertExtractor

object SemanticRuleSuite {
  def defaultClasspath(classDirectory: AbsolutePath): Classpath = Classpath(
    classDirectory ::
      RuleCompilerClasspath.defaultClasspathPaths.filter(path =>
        path.toNIO.getFileName.toString.contains("scala-library")
      )
  )

  def stripTestkitComments(input: String): String =
    stripTestkitComments(input.tokenize.get)

  def stripTestkitComments(tokens: Tokens): String = {
    val configComment = findTestkitComment(tokens)
    tokens.filter {
      case `configComment` => false
      case EndOfLineAssertExtractor(_) => false
      case MultiLineAssertExtractor(_) => false
      case _ => true
    }.mkString
  }

  def findTestkitComment(tokens: Tokens): Token = {
    tokens
      .find { x =>
        x.is[Token.Comment] && x.syntax.startsWith("/*")
      }
      .getOrElse {
        val input = tokens.headOption.fold("the file")(_.input.syntax)
        throw new IllegalArgumentException(
          s"Missing /* */ comment at the top of $input"
        )
      }
  }

}
