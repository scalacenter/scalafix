/*
rules = "ExplicitResultTypes"
ExplicitResultTypes.skipSimpleDefinitions = ["Lit"]
 */
package test.explicitResultTypes

object ExplicitResultTypesTrait {
  trait Trait {
    def foo: Map[Int, String]
    def message: CharSequence
  }

  object Overrides extends Trait {
    val foo = Map.empty
    val message = s"hello $foo"
  }
}
