/*
rules = [
  "Scala3NewSyntax"
]
*/
package scala3NewSyntax

object Underscore:
  val list: List[_] = List("")
  val other : Map[_ <: AnyRef, _ >: Null]  = ???