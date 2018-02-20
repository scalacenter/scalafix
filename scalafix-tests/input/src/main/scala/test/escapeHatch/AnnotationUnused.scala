/*
rules = [
  "class:scalafix.test.EscapeHatchDummyLinter"
  "class:scalafix.test.EscapeHatchNoNulls"
]
*/
package test.escapeHatch

object AnnotationUnused {

  @SuppressWarnings(Array("scalafix:EscapeHatchDummyLinter")) // assert: UnusedScalafixSuppression.Disable
  def a = ()

  @SuppressWarnings(Array("scalafix:EscapeHatchDummyLinter", "EscapeHatchNoNulls")) // assert: UnusedScalafixSuppression.Disable
  def b = ()

  @SuppressWarnings(Array("scalafix:EscapeHatchDummyLinter")) // assert: UnusedScalafixSuppression.Disable
  @SuppressWarnings(Array("scalafix:EscapeHatchNoNulls")) // assert: UnusedScalafixSuppression.Disable
  val c = 0

  @SuppressWarnings(Array("EscapeHatchDummyLinter", "EscapeHatchNoNulls")) // OK, not prefixed
  def d = ()

  @SuppressWarnings(Array("scalafix:EscapeHatchDummyLinter")) // assert: UnusedScalafixSuppression.Disable
  object Foo {

    @SuppressWarnings(Array("all")) // OK, not prefixed
    def bar = ???
  }
}
