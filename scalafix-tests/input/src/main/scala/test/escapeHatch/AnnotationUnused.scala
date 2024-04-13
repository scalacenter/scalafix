/*
rules = [
  "class:scalafix.test.NoDummy"
  "class:scalafix.test.NoNull"
]
*/
package test.escapeHatch

object AnnotationUnused {

  @SuppressWarnings(Array("scalafix:NoDummy")) // assert: UnusedScalafixSuppression
  def a = ()

  @SuppressWarnings(Array("scalafix:NoDummy", "NoNull")) // assert: UnusedScalafixSuppression
  def b = ()

  @SuppressWarnings(Array("scalafix:NoDummy", "scalafix:NoNull")) // assert: UnusedScalafixSuppression
  val c = 0

  @SuppressWarnings(Array("NoDummy", "NoNull")) // OK, not prefixed
  def d = ()

  @SuppressWarnings(Array("""scalafix:NoDummy""")) // assert: UnusedScalafixSuppression
  def tripleQuotes = ()

  @SuppressWarnings(Array("scalafix:NoDummy")) // assert: UnusedScalafixSuppression
  object Foo {

    @SuppressWarnings(Array("all")) // OK, not prefixed
    def bar = ???
  }
}
