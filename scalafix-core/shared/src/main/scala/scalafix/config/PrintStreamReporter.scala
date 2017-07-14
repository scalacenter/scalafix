package scalafix.config

import scala.meta.Position
import scala.meta.internal.inputs.XtensionPositionFormatMessage
import java.io.PrintStream
import scalafix.internal.util.Severity
import metaconfig._

/** A ScalafixReporter that emits messages to a PrintStream. */
case class PrintStreamReporter(
    outStream: PrintStream,
    minSeverity: Severity,
    filter: FilterMatcher,
    includeLoggerName: Boolean)
    extends ScalafixReporter {
  val reader: ConfDecoder[ScalafixReporter] =
    ConfDecoder.instanceF[ScalafixReporter] { c =>
      (
        c.getOrElse("minSeverity")(minSeverity) |@|
          c.getOrElse("filter")(filter) |@|
          c.getOrElse("includeLoggerName")(includeLoggerName)
      ).map {
        case ((a, b), c) =>
          copy(
            minSeverity = a,
            filter = b,
            includeLoggerName = c
          )
      }
    }

  override def report(message: String, position: Position, severity: Severity)(
      implicit ctx: LogContext): Unit = {
    val enclosing =
      if (includeLoggerName) s"(${ctx.enclosing.value}) " else ""
    outStream.println(
      position.formatMessage(enclosing + severity.toString, message))
  }
}
