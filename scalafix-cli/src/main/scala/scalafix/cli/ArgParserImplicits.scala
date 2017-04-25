package scalafix
package cli

import scalafix.config.ScalafixConfig
import scalafix.config.ScalafixMetaconfigReaders

import java.io.InputStream
import java.io.PrintStream

import caseapp.core.ArgParser
import caseapp.core.Messages
import caseapp.core.Parser
import metaconfig.Conf

object ArgParserImplicits {
  implicit val inputStreamRead: ArgParser[InputStream] =
    ArgParser.instance[InputStream](_ => Right(System.in))
  implicit val printStreamRead: ArgParser[PrintStream] =
    ArgParser.instance[PrintStream](_ => Right(System.out))
  val OptionsParser: Parser[ScalafixOptions] = Parser.apply[ScalafixOptions]
  val OptionsMessages: Messages[ScalafixOptions] = Messages[ScalafixOptions]
}
