package scalafix.testkit

import scala.meta._
import scala.meta.internal.inputs.XtensionInputSyntaxStructure

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

  private[testkit] def testKitCommentPredicate(token: Token): Boolean =
    token.is[Token.Comment] && token.syntax.startsWith("/*")

  def findTestkitComment(tokens: Tokens): Token = {
    tokens
      .find(testKitCommentPredicate)
      .getOrElse {
        val input = tokens.headOption.fold("the file")(_.input.syntax)
        throw new IllegalArgumentException(
          s"Missing /* */ comment at the top of $input"
        )
      }
  }

  def filterPossibleTestkitComments(tokens: Tokens): IndexedSeq[Token] =
    tokens.filter(testKitCommentPredicate)

}
