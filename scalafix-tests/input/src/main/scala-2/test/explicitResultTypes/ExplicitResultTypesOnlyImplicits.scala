/*
rules = ExplicitResultTypes
ExplicitResultTypes.onlyImplicits = true
 */
package test.explicitResultTypes

object ExplicitResultTypesOnlyImplicits {
  def complex = List(1).map(_ + 1)
  implicit val default = complex
}
