/*
rules = "class:scalafix.test.NoDummy"
*/
package test.escapeHatch

object AnnotationWithNonLiteralValue {

  final val RuleName = "NoDummy"

  @SuppressWarnings(Array(RuleName)) // <- has no effect as the rule name must be a literal
  val aDummy = 0 // assert: NoDummy
}
