package scalafix.cli

import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scalafix.config.ScalafixConfig
import scalafix.config.ScalafixMetaconfigReaders
import scalafix.rewrite.ScalafixRewrite
import scalafix.rewrite.ScalafixRewrites

import java.io.File
import java.io.InputStream
import java.io.PrintStream

import caseapp.core.ArgParser
import caseapp.core.Parser
import caseapp._
import caseapp.core.Messages
import org.scalameta.logger

object ArgParserImplicits {

  implicit val inputStreamRead: ArgParser[InputStream] =
    ArgParser.instance[InputStream](_ => Right(System.in))

  implicit val printStreamRead: ArgParser[PrintStream] =
    ArgParser.instance[PrintStream](_ => Right(System.out))

  implicit val ScalafixConfigParser: ArgParser[ScalafixConfig] =
    ArgParser.instance[ScalafixConfig] { str =>
      Try(new File(str)) match {
        case Success(file) if file.isFile && file.exists() =>
          ScalafixConfig.fromFile(file).left.map(_.getMessage)
        case _ =>
          ScalafixConfig.fromString(str).left.map(_.getMessage)
      }
    }

  implicit val rewriteRead: ArgParser[ScalafixRewrite] =
    ArgParser.instance[ScalafixRewrite] { str =>
      ScalafixMetaconfigReaders.rewriteReader.read(str).left.map(_.getMessage)
    }

  val OptionsParser: Parser[ScalafixOptions] = Parser.apply[ScalafixOptions]
  val OptionsMessages: Messages[ScalafixOptions] = Messages[ScalafixOptions]
}
