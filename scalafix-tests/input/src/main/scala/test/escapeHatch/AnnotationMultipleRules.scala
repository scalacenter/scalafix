/*
rules = [
  Disable
  "class:scalafix.test.EscapeHatchDummyLinter"
  "class:scalafix.test.EscapeHatchNoNulls"
]

Disable.symbols = [
  "scala.Option.get"
]
*/
package test.escapeHatch

object AnnotationMultipleRules {

  def aDummy0(x: Option[Any]): Unit = { // assert: EscapeHatchDummyLinter
    val y = null // assert: EscapeHatchNoNulls
    val z = x.get // assert: Disable.get
  }

  // single annotation
  @SuppressWarnings(Array("Disable.get", "EscapeHatchDummyLinter", "EscapeHatchNoNulls"))
  def aDummy1(x: Option[Any]): Unit = {
    val y = null
    val z = x.get
  }

  // annotations in same element
  @SuppressWarnings(Array("Disable.get"))
  @SuppressWarnings(Array("EscapeHatchDummyLinter"))
  @SuppressWarnings(Array("EscapeHatchNoNulls"))
  def aDummy2(x: Option[Any]): Unit = {
    val y = null
    val z = x.get
  }

  // annotations in individual elements
  @SuppressWarnings(Array("EscapeHatchDummyLinter"))
  def aDummy3(x: Option[Any]): Unit = {
    @SuppressWarnings(Array("EscapeHatchNoNulls"))
    val y = null
    @SuppressWarnings(Array("Disable.get"))
    val z = x.get
  }
}
