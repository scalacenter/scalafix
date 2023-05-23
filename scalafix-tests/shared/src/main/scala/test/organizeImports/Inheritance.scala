package test.organizeImports

class SomeClass {
  val any: Any = ???
}

trait SomeTrait {
  val field = new SomeClass
}

object SomeObject extends SomeTrait
