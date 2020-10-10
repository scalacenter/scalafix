package scalafix.internal.compat

import scala.tools.nsc.interactive.Global

import scala.meta.internal.pc.ScalafixGlobal

object CompilerCompat {
  implicit class XtensionGlobal(global: Global) {
    def closeCompat(): Unit = {
      global.close()
    }
  }
  // in 2.13 Serializable trait with type alias
  def serializableClass(g: ScalafixGlobal): Set[g.ClassSymbol] =
    Set(g.definitions.SerializableClass)
}
