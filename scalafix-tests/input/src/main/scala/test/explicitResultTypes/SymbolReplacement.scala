/*
rules = ExplicitResultTypes
ExplicitResultTypes.skipSimpleDefinitions = false
ExplicitResultTypes.symbolReplacements {
  "test/explicitResultTypes/SymbolReplacement.DefaultTimer." = 
    "test/explicitResultTypes/SymbolReplacement.Timer#"
  "test/explicitResultTypes/SymbolReplacement.Dog#" = 
    "test/explicitResultTypes/SymbolReplacement.Animal#"
}

*/
package test.explicitResultTypes

object SymbolReplacement {
  trait Timer
  object DefaultTimer extends Timer
  def myTimer = DefaultTimer

  trait Animal
  class Dog extends Animal
  val dog = new Dog()
}
