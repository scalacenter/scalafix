package test.explicitResultTypes

object SymbolReplacement {
  trait Timer
  object DefaultTimer extends Timer
  def myTimer: Timer = DefaultTimer

  trait Animal
  class Dog extends Animal
  val dog: Animal = new Dog()
}

