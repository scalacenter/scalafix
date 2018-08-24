package scalafix.internal.config

import java.io.PrintStream
import scala.meta.Position
import scalafix.internal.util.PositionSyntax._
import scalafix.lint.RuleDiagnostic
import scalafix.lint.LintSeverity

/** A ScalafixReporter that emits messages to a PrintStream. */
case class PrintStreamReporter(
    out: PrintStream
) extends ScalafixReporter {
  override def lint(d: RuleDiagnostic): Unit = {
    report(s"[${d.id.fullID}] ${d.message}", d.position, d.severity)
  }

  override private[scalafix] def report(
      message: String,
      position: Position,
      severity: LintSeverity): Unit = {
    val formatted = position.formatMessage(severity.toString, message)
    out.println(formatted)
  }
}

object PrintStreamReporter {
  def default: PrintStreamReporter = PrintStreamReporter(System.out)
}
