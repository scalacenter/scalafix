package scalafix.internal.v1

import java.util.concurrent.atomic.AtomicInteger

import scalafix.interfaces
import scalafix.interfaces.ScalafixDiagnostic
import scalafix.interfaces.ScalafixMainCallback

final class DelegatingMainCallback(underlying: ScalafixMainCallback)
    extends ScalafixMainCallback {
  private val lintErrorCount = new AtomicInteger()
  private val normalErrorCount = new AtomicInteger()
  private val lintCount = new AtomicInteger()
  def hasErrors: Boolean = normalErrorCount.get() > 0
  def hasLintErrors: Boolean = lintErrorCount.get() > 0
  // any severity, unlike hasLintErrors
  def hasLintDiagnostics: Boolean = lintCount.get() > 0
  override def reportDiagnostic(diagnostic: ScalafixDiagnostic): Unit = {
    if (diagnostic.lintID().isPresent) lintCount.incrementAndGet()
    if (diagnostic.severity() == interfaces.ScalafixSeverity.ERROR) {
      if (diagnostic.lintID().isPresent) {
        lintErrorCount.incrementAndGet()
      } else {
        normalErrorCount.incrementAndGet()
      }
    }
    underlying.reportDiagnostic(diagnostic)
  }
}
