package scalafix.cli

import scalafix.rewrite.ProcedureSyntax
import scalafix.rewrite.Rewrite

import java.io.InputStream
import java.io.PrintStream

import caseapp.core.ArgParser

object ArgParserImplicits {
  def nameMap[T](t: sourcecode.Text[T]*): Map[String, T] = {
    t.map(x => x.source -> x.value).toMap
  }

  val rewriteMap: Map[String, Rewrite] = nameMap(
    ProcedureSyntax
  )
  implicit val rewriteRead: ArgParser[Rewrite] = ArgParser.instance[Rewrite] {
    str =>
      rewriteMap.get(str) match {
        case Some(x) => Right(x)
        case _ =>
          Left(
            s"invalid input $str, must be one of ${rewriteMap.keys.mkString(", ")}")
      }
  }

  implicit val inputStreamRead: ArgParser[InputStream] =
    ArgParser.instance[InputStream](x => Right(System.in))

  implicit val printStreamRead: ArgParser[PrintStream] =
    ArgParser.instance[PrintStream](x => Right(System.out))

}
