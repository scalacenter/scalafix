package scalafix.internal.compat

import scala.tools.nsc.interactive.Global

object CompilerCompat {
  implicit class XtensionGlobal(global: Global) {
    def closeCompat(): Unit = {} // Do nothing, method is missing in 2.11
  }
}
