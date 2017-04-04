package scalafix.util

import scala.meta.Tree
import scala.meta.prettyprinters.Structure

import java.io.File

/**
  * Debugging utility.
  */
object logger {

  private def log[T](t: sourcecode.Text[T],
                     logLevel: LogLevel,
                     line: sourcecode.Line,
                     file: sourcecode.File,
                     enclosing: sourcecode.Enclosing,
                     showSource: Boolean): Unit = {
    val position = f"${new File(file.value).getName}:${line.value}"
    val value = {
      val str = s"${t.value}"
      if (str.contains("\n")) s"\n$str"
      else str
    }
    val key =
      if (showSource) s"[${t.source}]: "
      else ""
    println(f"$logLevel%-7s $position%-25s $key$value")
  }

  def elem(ts: sourcecode.Text[Any]*)(
      implicit line: sourcecode.Line,
      file: sourcecode.File,
      enclosing: sourcecode.Enclosing): Unit = {
    ts.foreach { t =>
      log(t, LogLevel.debug, line, file, enclosing, showSource = true)
    }
  }

  def trace[T](t: => sourcecode.Text[T])(
      implicit line: sourcecode.Line,
      file: sourcecode.File,
      enclosing: sourcecode.Enclosing): Unit =
    Unit

  def debug[T](t: sourcecode.Text[T])(implicit line: sourcecode.Line,
                                      file: sourcecode.File,
                                      enclosing: sourcecode.Enclosing): Unit =
    log(t, LogLevel.debug, line, file, enclosing, showSource = false)

  def info[T](t: sourcecode.Text[T])(implicit line: sourcecode.Line,
                                     file: sourcecode.File,
                                     enclosing: sourcecode.Enclosing): Unit =
    log(t, LogLevel.info, line, file, enclosing, showSource = false)

  def warn[T](t: sourcecode.Text[T])(implicit line: sourcecode.Line,
                                     file: sourcecode.File,
                                     enclosing: sourcecode.Enclosing): Unit =
    log(t, LogLevel.warn, line, file, enclosing, showSource = false)

  def error[T](t: sourcecode.Text[T])(implicit line: sourcecode.Line,
                                      file: sourcecode.File,
                                      enclosing: sourcecode.Enclosing): Unit =
    log(t, LogLevel.error, line, file, enclosing, showSource = false)

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
