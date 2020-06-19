package scalafix.internal.compat

import scala.meta.internal.pc.ScalafixGlobal
import scala.tools.nsc.interactive.Global

object CompilerCompat {
  implicit class XtensionGlobal(global: Global) {
    def closeCompat(): Unit = {} // Do nothing, method is missing in 2.11
  }

  def serializableClass(g: ScalafixGlobal): Set[g.ClassSymbol] =
    Set(g.definitions.SerializableClass, g.definitions.JavaSerializableClass)
}
