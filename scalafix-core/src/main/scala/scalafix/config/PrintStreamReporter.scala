package scalafix.config

import scalafix.util.Severity
import scala.meta.Position
import scala.meta.internal.inputs._

import java.io.PrintStream

import metaconfig.ConfigReader
import metaconfig.Reader

/** A ScalafixReporter that emits messages to a PrintStream. */
@ConfigReader
case class PrintStreamReporter(outStream: PrintStream,
                               minSeverity: Severity,
                               filter: FilterMatcher,
                               includeLoggerName: Boolean)
    extends ScalafixReporter {
  implicit val FilterMatcherReader: Reader[FilterMatcher] = filter.reader

  override def report(message: String, position: Position, severity: Severity)(
      implicit ctx: LogContext): Unit = {
    val enclosing =
      if (includeLoggerName) s"(${ctx.enclosing.value}) " else ""
    outStream.println(
      position.start.formatMessage(enclosing + severity.toString, message))
  }
}
