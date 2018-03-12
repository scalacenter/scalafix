/*
rules = [
  "class:scalafix.test.NoDummy"
  "class:scalafix.test.NoNull"
]

Disable.symbols = [
  "scala.Option.get"
]
*/
package test.escapeHatch

object AnnotationFullyQualified {

  @java.lang.SuppressWarnings(Array("scalafix:NoDummy"))
  @java.lang.SuppressWarnings(Array("scalafix:NoNull"))
  def aDummy(x: Option[Any]): Unit = {
    val y = null
  }
}
