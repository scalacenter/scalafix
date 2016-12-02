package scalafix.nsc

trait ReflectToolkit {
  val global: scala.tools.nsc.Global
  lazy val g: global.type = global
}
