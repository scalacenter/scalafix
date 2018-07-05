/*
rules = [
  Disable
  "class:scalafix.test.NoDummy"
  "class:scalafix.test.NoNull"
]

Disable.symbols = [
  "scala.Option.get"
]
*/
package test.escapeHatch

object AnnotationMultipleRules {

  def aDummy0(x: Option[Any]): Unit = { // assert: NoDummy
    val y = null // assert: NoNull
    val z = x.get // assert: Disable.get
  }

  // single annotation
  @SuppressWarnings(Array("Disable.get", "NoDummy", "NoNull"))
  def aDummy1(x: Option[Any]): Unit = {
    val y = null
    val z = x.get
  }

  // annotations in individual elements
  @SuppressWarnings(Array("NoDummy"))
  def aDummy2(x: Option[Any]): Unit = {
    @SuppressWarnings(Array("NoNull"))
    val y = null
    @SuppressWarnings(Array("Disable.get"))
    val z = x.get
  }
}
