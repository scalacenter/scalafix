package test.explicitResultTypes

object SymbolReplacement {
  trait Timer
  object DefaultTimer extends Timer
  def myTimer: DefaultTimer.type = DefaultTimer

  trait Animal
  class Dog extends Animal
  val dog: Dog = new Dog()
}

