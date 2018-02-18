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

object AnnotationPrefixedRules {

  def aDummy0(x: Option[Any]): Unit = { // assert: EscapeHatchDummyLinter
    val y = null // assert: EscapeHatchNoNulls
    val z = x.get // assert: Disable.get
  }

  @SuppressWarnings(Array("scalafix:Disable.get"))
  @SuppressWarnings(Array("scalafix:EscapeHatchDummyLinter"))
  @SuppressWarnings(Array("scalafix:EscapeHatchNoNulls"))
  def aDummy1(x: Option[Any]): Unit = {
    val y = null
    val z = x.get
  }
}
