package scalafix.internal.compat

import scala.tools.nsc.interactive.Global

import scala.meta.internal.pc.ScalafixGlobal

object CompilerCompat {
  implicit class XtensionGlobal(global: Global) {
    def closeCompat(): Unit = {
      global.close()
    }
  }

  def serializableClass(g: ScalafixGlobal): Set[g.ClassSymbol] =
    Set(g.definitions.SerializableClass, g.definitions.JavaSerializableClass)
}
