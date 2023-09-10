package scalafix.testkit

import scala.util.Try

import scala.meta._
import scala.meta.internal.inputs.XtensionInputSyntaxStructure

import metaconfig.Conf
import metaconfig.internal.ConfGet
import metaconfig.typesafeconfig.typesafeConfigMetaconfigParser
import scalafix.internal.config.ScalafixConfig
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

  def findTestkitComment(tokens: Tokens): Token =
    parseTestkitComment(tokens)._1

  def parseTestkitComment(
      tokens: Tokens
  ): (Token, Conf, Conf, ScalafixConfig) = {
    def extractTestkitHints(comment: Token) = {
      val syntax = comment.syntax.stripPrefix("/*").stripSuffix("*/")
      for {
        conf <- Try(Conf.parseString("comment", syntax)).toOption
          .flatMap(_.toEither.toOption)
        rulesConf <- ConfGet.getKey(conf, "rules" :: "rule" :: Nil)
        scalafixConfig <- conf.as[ScalafixConfig].toEither.toOption
      } yield (comment, conf, rulesConf, scalafixConfig)
    }

    // It is possible to have comments which are not valid HOCON with
    // rules (i.e. license headers), so lets parse until we find one
    tokens
      .filter(token => token.is[Token.Comment] && token.syntax.startsWith("/*"))
      .collectFirst(Function.unlift(extractTestkitHints))
      .getOrElse {
        val input = tokens.headOption.fold("the file")(_.input.syntax)
        throw new IllegalArgumentException(
          s"Missing /* */ comment with rules attribute in $input"
        )
      }
  }

}
