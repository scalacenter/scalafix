package scalafix.internal.cli

import java.io.InputStream
import java.io.PrintStream
import caseapp.core.ArgParser
import caseapp.core.Messages
import caseapp.core.Parser

object ArgParserImplicits {
  implicit val inputStreamRead: ArgParser[InputStream] =
    ArgParser.instance[InputStream](_ => Right(System.in))
  implicit val printStreamRead: ArgParser[PrintStream] =
    ArgParser.instance[PrintStream](_ => Right(System.out))
  val OptionsParser: Parser[ScalafixOptions] = Parser.apply[ScalafixOptions]
  val OptionsMessages: Messages[ScalafixOptions] = Messages[ScalafixOptions]
}
