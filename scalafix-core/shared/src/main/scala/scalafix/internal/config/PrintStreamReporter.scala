package scalafix.internal.config

import java.io.OutputStream
import scala.meta.Position
import java.io.PrintStream
import java.util.concurrent.atomic.AtomicInteger
import scalafix.internal.util.Severity
import metaconfig._
import org.langmeta.internal.ScalafixLangmetaHacks
import MetaconfigPendingUpstream._

/** A ScalafixReporter that emits messages to a PrintStream. */
case class PrintStreamReporter(
    outStream: PrintStream,
    minSeverity: Severity,
    filter: FilterMatcher,
    includeLoggerName: Boolean,
    format: OutputFormat
) extends ScalafixReporter {
  val reader: ConfDecoder[ScalafixReporter] =
    ConfDecoder.instanceF[ScalafixReporter] { c =>
      (
        c.getField(minSeverity) |@|
          c.getField(filter) |@|
          c.getField(includeLoggerName) |@|
          c.getField(format)
      ).map {
        case (((a, b), c), d) =>
          copy(
            minSeverity = a,
            filter = b,
            includeLoggerName = c,
            format = d
          )
      }
    }
  private val _errorCount = new AtomicInteger()
  override def reset: PrintStreamReporter = copy()
  override def reset(os: OutputStream): ScalafixReporter = os match {
    case ps: PrintStream => copy(outStream = ps)
    case _ => copy(outStream = new PrintStream(os))
  }
  override def withFormat(format: OutputFormat): ScalafixReporter =
    copy(format = format)

  override def report(message: String, position: Position, severity: Severity)(
      implicit ctx: LogContext): Unit = {
    if (severity == Severity.Error) {
      _errorCount.incrementAndGet()
    }
    val enclosing =
      if (includeLoggerName) s"(${ctx.enclosing.value}) " else ""
    val gutter = format match {
      case OutputFormat.Default => ""
      case OutputFormat.Sbt => s"[$severity] "
    }
    val formatted = ScalafixLangmetaHacks.formatMessage(
      position,
      enclosing + severity.toString,
      message)
    outStream.println(gutter + formatted)
  }

  /** Returns true if this reporter has seen an error */
  override def errorCount: Int = _errorCount.get()
}
