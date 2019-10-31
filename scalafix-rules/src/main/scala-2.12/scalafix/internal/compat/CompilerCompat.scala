package scalafix.internal.compat

import scala.tools.nsc.interactive.Global

object CompilerCompat {
  implicit class XtensionGlobal(global: Global) {
    def closeCompat(): Unit = {
      global.close()
    }
  }
}
