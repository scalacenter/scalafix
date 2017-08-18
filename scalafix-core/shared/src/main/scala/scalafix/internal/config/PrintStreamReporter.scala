package scalafix.internal.config

import scala.meta.Position
import scala.meta.internal.inputs.XtensionPositionFormatMessage
import java.io.PrintStream
import java.util.concurrent.atomic.AtomicInteger
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
  private val _errorCount = new AtomicInteger()
  override def reset: PrintStreamReporter = copy()

  override def report(message: String, position: Position, severity: Severity)(
      implicit ctx: LogContext): Unit = {
    if (severity == Severity.Error) {
      _errorCount.incrementAndGet()
    }
    val enclosing =
      if (includeLoggerName) s"(${ctx.enclosing.value}) " else ""
    outStream.println(
      position.formatMessage(enclosing + severity.toString, message))
  }

  /** Returns true if this reporter has seen an error */
  override def errorCount: Int = _errorCount.get()
}
