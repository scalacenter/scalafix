/*
rules = [
  "class:scalafix.test.NoDummy"
  "class:scalafix.test.NoNull"
]
*/
package test.escapeHatch

object AnnotationUnused {

  @SuppressWarnings(Array("scalafix:NoDummy")) // assert: UnusedScalafixSuppression.Disable
  def a = ()

  @SuppressWarnings(Array("scalafix:NoDummy", "NoNull")) // assert: UnusedScalafixSuppression.Disable
  def b = ()

  @SuppressWarnings(Array("scalafix:NoDummy")) // assert: UnusedScalafixSuppression.Disable
  @SuppressWarnings(Array("scalafix:NoNull")) // assert: UnusedScalafixSuppression.Disable
  val c = 0

  @SuppressWarnings(Array("NoDummy", "NoNull")) // OK, not prefixed
  def d = ()

  @SuppressWarnings(Array("scalafix:NoDummy")) // assert: UnusedScalafixSuppression.Disable
  object Foo {

    @SuppressWarnings(Array("all")) // OK, not prefixed
    def bar = ???
  }
}
