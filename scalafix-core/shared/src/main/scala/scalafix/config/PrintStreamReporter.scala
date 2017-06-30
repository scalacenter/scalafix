package scalafix.config

import scala.meta.Position
import scala.meta.internal.inputs.XtensionPointFormatMessage
import java.io.PrintStream
import scalafix.internal.util.Severity
import metaconfig.Recurse
import metaconfig._

/** A ScalafixReporter that emits messages to a PrintStream. */
@DeriveConfDecoder
case class PrintStreamReporter(outStream: PrintStream,
                               minSeverity: Severity,
                               @Recurse filter: FilterMatcher,
                               includeLoggerName: Boolean)
    extends ScalafixReporter {
  private val FilterMatcherReader = null // shadow other reader

  override def report(message: String, position: Position, severity: Severity)(
      implicit ctx: LogContext): Unit = {
    val enclosing =
      if (includeLoggerName) s"(${ctx.enclosing.value}) " else ""
    outStream.println(
      position.start.formatMessage(enclosing + severity.toString, message))
  }
}
