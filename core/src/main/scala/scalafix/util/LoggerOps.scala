package scalafix.util

import scala.meta.Tree
import scala.meta.prettyprinters.Structure
import scala.meta.tokens.Token
import scala.meta.tokens.Token.Interpolation
import scala.meta.tokens.Tokens

/**
  * Debugging utility.
  */
object LoggerOps {
  val logger = PrintlnLogger

  def log(t: Tree, tokensOnly: Boolean = false): String = {
    val tokens =
      s"TOKENS: ${t.tokens.map(x => reveal(x.syntax)).mkString(",")}"
    if (tokensOnly) tokens
    else s"""TYPE: ${t.getClass.getName.stripPrefix("scala.meta.")}
             |SOURCE: $t
             |STRUCTURE: ${t.show[Structure]}
             |$tokens
             |""".stripMargin
  }

  def reveal(s: String): String = s.map {
    case '\n' => '¶'
    case ' ' => '∙'
    case ch => ch
  }

  def header[T](t: T): String = {
    val line = s"=" * (t.toString.length + 3)
    s"$line\n=> $t\n$line"
  }
}
