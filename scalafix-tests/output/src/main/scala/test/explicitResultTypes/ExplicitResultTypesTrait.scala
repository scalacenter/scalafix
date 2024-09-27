package test.explicitResultTypes

object ExplicitResultTypesTrait {
  trait Trait {
    def foo: Map[Int, String]
    def message: CharSequence
  }

  object Overrides extends Trait {
    val foo: Map[Int,String] = Map.empty
    val message: String = s"hello $foo"
  }
}
