package scalafix.internal.cli

import java.io.InputStream
import java.io.PrintStream
import scalafix.internal.config.OutputFormat
import caseapp.core.ArgParser
import caseapp.core.Messages
import caseapp.core.Parser

object ArgParserImplicits {
  implicit val inputStreamRead: ArgParser[InputStream] =
    ArgParser.instance[InputStream]("stdin")(_ => Right(System.in))
  implicit val printStreamRead: ArgParser[PrintStream] =
    ArgParser.instance[PrintStream]("stdout")(_ => Right(System.out))
  implicit val formatParser: ArgParser[OutputFormat] =
    ArgParser.instance[OutputFormat]("format")(OutputFormat.apply)
  val OptionsParser: Parser[ScalafixOptions] = Parser.apply[ScalafixOptions]
  val OptionsMessages: Messages[ScalafixOptions] = Messages[ScalafixOptions]
}
