package test.organizeImports

object GivenImports {
  trait Alpha
  trait Beta
  trait Gamma
  trait Delta
  trait Zeta

  given alpha: Alpha = ???
  given beta: Beta = ???
  given gamma: Gamma = ???
  given delta: Delta = ???
  given zeta: Zeta = ???
}

object GivenImports2 {
  import GivenImports.*

  given alpha: Alpha = ???
  given beta: Beta = ???
  given gamma: Gamma = ???
  given delta: Delta = ???
  given zeta: Zeta = ???
}
