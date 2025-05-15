/*
rules = DisableSyntax
DisableSyntax.noAsInstanceOf = true
*/
package test.disableSyntax

object MatchableAsInstanceOf {
  class Example {
    override def equals(obj: Any): Boolean =
      obj.asInstanceOf[Matchable] match { /* assert: DisableSyntax.asInstanceOfMatchable
         ^^^^^^^^^^^^^
      asInstanceOf[Matchable] is used here to enable pattern matching on Any. Consider using the .asMatchable extension method instead for better readability.
      */
        case that: Example => true
        case _ => false
      }
  }

  def regularCast(x: Any): String =
    x.asInstanceOf[String] /* assert: DisableSyntax.asInstanceOf
      ^^^^^^^^^^^^
    asInstanceOf casts are disabled, use pattern matching instead
    */

  // whitespace between tokens
  class WhitespaceExample {
    override def equals(obj: Any): Boolean =
      obj.asInstanceOf  [  Matchable  ] match { /* assert: DisableSyntax.asInstanceOf
          ^^^^^^^^^^^^
      asInstanceOf casts are disabled, use pattern matching instead
      */
        case _ => true
      }
  }
}
