package scalafix.testkit

import scala.util.Try

import scala.meta._
import scala.meta.internal.inputs.XtensionInput
import scala.meta.tokens.Token

import metaconfig.Conf
import metaconfig.ConfError
import metaconfig.Configured
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
    val configComment = parseTestkitCommentOpt(tokens).fold(null: Token)(_._1)
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
    parseTestkitCommentOpt(tokens).getOrElse {
      val input = tokens.headOption.fold("the file")(_.input.syntax)
      throw new IllegalArgumentException(
        s"Missing /* */ comment with rules attribute in $input"
      )
    }
  }

  private[scalafix] def parseTestkitCommentOpt(
      tokens: Tokens
  ): Option[(Token.Comment, Conf, Conf, ScalafixConfig)] = {
    def extractTestkitHints(token: Token) = token match {
      case comment: Token.Comment =>
        val origSyntax = comment.syntax
        val syntaxNoPrefix = origSyntax.stripPrefix("/*")
        if (origSyntax eq syntaxNoPrefix) None
        else {
          val syntax = syntaxNoPrefix.stripSuffix("*/")
          for {
            conf <- Try(Conf.parseString("comment", syntax)).toOption
              .flatMap(_.toEither.toOption)
            rulesConf <- ConfGet
              .getOrElse(
                Configured.ok,
                ConfError.empty.notOk
              )(conf, "rules" :: "rule" :: Nil)
              .toEither
              .toOption
            scalafixConfig <- conf.as[ScalafixConfig].toEither.toOption
          } yield (comment, conf, rulesConf, scalafixConfig)
        }
      case _ => None
    }

    // It is possible to have comments which are not valid HOCON with
    // rules (i.e. license headers), so lets parse until we find one
    val iter = tokens.iterator.flatMap(extractTestkitHints)
    if (iter.hasNext) Some(iter.next()) else None
  }

}
