package scala.scalajs.js.annotation

class JSGlobal extends scala.annotation.StaticAnnotation {
  def this(name: String) = this()
}

class JSImport(module: String, name: String)
    extends scala.annotation.StaticAnnotation {
  def this(module: String, name: JSImport.Namespace.type) =
    this(module, null: String)
}

object JSImport {
  final val Default = "default"
  object Namespace
}

class JSGlobalScope extends scala.annotation.StaticAnnotation
