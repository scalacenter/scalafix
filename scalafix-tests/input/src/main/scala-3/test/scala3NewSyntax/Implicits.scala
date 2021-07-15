/*
rules = [
  "Scala3NewSyntax"
]
*/
package scala3NewSyntax

object Implicits:
  implicit val text: String = "text"/* assert: Scala3NewSyntax.Implicits
  ^^^^^^^^
  Implicit can be replace with using/given
  */