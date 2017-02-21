package scalafix.cli

import scalafix.rewrite.Rewrite
import scalafix.rewrite.ScalafixRewrite

import java.io.InputStream
import java.io.PrintStream

import caseapp.core.ArgParser

object ArgParserImplicits {

  implicit val rewriteRead: ArgParser[ScalafixRewrite] =
    ArgParser.instance[ScalafixRewrite] { str =>
      Rewrite.name2rewrite.get(str) match {
        case Some(x) => Right(x)
        case _ =>
          val availableKeys = Rewrite.name2rewrite.keys.mkString(", ")
          Left(s"invalid input $str, must be one of $availableKeys")
      }
    }

  implicit val inputStreamRead: ArgParser[InputStream] =
    ArgParser.instance[InputStream](x => Right(System.in))

  implicit val printStreamRead: ArgParser[PrintStream] =
    ArgParser.instance[PrintStream](x => Right(System.out))

}
