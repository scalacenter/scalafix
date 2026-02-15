package test.organizeImports

object Givens {
  import GivenImports.*

  trait A
  trait B
  trait C
  trait D
  trait E

  given a: A = ???
  given b: B = ???
  given c: C = ???
  given d: D = ???
  given e: E = ???

  // For testing: given imports that reference types from GivenImports
  given alpha: Alpha = ???
  given beta: Beta = ???
}
