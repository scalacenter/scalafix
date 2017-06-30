package scalafix.config

import scala.meta.Position
import scalafix.internal.util.Severity
import metaconfig.ConfDecoder

trait ScalafixReporter {

  /** Messages with severity < minSeverity are skipped. */
  def minSeverity: Severity

  /** Messages whose enclosing scope don't match filter.matches are skipped. */
  def filter: FilterMatcher

  /** Present the message to the user.
    *
    * In a command-line interface, this might mean "print message to console".
    * In an IDE, this might mean putting red/yellow squiggly marks under code.
    */
  protected def report(message: String,
                       position: Position,
                       severity: Severity)(implicit ctx: LogContext): Unit

  protected def handleMessage(
      message: String,
      position: Position,
      severity: Severity)(implicit ctx: LogContext): Unit =
    if (severity >= minSeverity && filter.matches(ctx.enclosing.value)) {
      report(message, position, severity)
    } else {
      () // do nothing
    }

  // format: off
  final def trace(message: String, position: Position = Position.None)(implicit ctx: LogContext): Unit = handleMessage(message, position, Severity.Trace)
  final def debug(message: String, position: Position = Position.None)(implicit ctx: LogContext): Unit = handleMessage(message, position, Severity.Debug)
  final def info (message: String, position: Position = Position.None)(implicit ctx: LogContext): Unit = handleMessage(message, position, Severity.Info)
  final def warn (message: String, position: Position = Position.None)(implicit ctx: LogContext): Unit = handleMessage(message, position, Severity.Warn)
  final def error(message: String, position: Position = Position.None)(implicit ctx: LogContext): Unit = handleMessage(message, position, Severity.Error)
  // format: on
}

object ScalafixReporter {
  val default: PrintStreamReporter = PrintStreamReporter(
    Console.out,
    Severity.Info,
    FilterMatcher.matchEverything,
    includeLoggerName = false
  )
  implicit val scalafixReporterReader: ConfDecoder[ScalafixReporter] =
    default.reader.map(_.asInstanceOf[ScalafixReporter])
}
