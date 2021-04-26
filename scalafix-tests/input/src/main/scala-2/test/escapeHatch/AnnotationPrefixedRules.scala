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

object AnnotationPrefixedRules {

  def aDummy0(x: Option[Any]): Unit = { // assert: NoDummy
    val y = null // assert: NoNull
    val z = x.get // assert: Disable.get
  }

  @SuppressWarnings(Array("scalafix:Disable.get", "scalafix:NoDummy", "scalafix:NoNull"))
  def aDummy1(x: Option[Any]): Unit = {
    val y = null
    val z = x.get
  }
}
