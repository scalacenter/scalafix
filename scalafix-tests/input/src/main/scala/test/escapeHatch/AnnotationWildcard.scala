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

object AnnotationWildcard {

  def aDummy0(x: Option[Any]): Unit = { // assert: NoDummy
    val y = null // assert: NoNull
    val z = x.get // assert: Disable.get
  }

  @SuppressWarnings(Array("all"))
  def aDummy1(x: Option[Any]): Unit = {
    val y = null
    val z = x.get
  }
}
