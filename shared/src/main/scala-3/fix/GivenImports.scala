package fix

object GivenImports {
  trait Alpha
  trait Beta
  trait Gamma
  trait Zeta
  
  given alpha: Alpha with {}
  given beta: Beta with {}
  given gamma: Gamma with {}
  given zeta: Zeta with {}
}

object GivenImports2 {
  import GivenImports.*
  
  given alpha: Alpha with {}
  given beta: Beta with {}
  given gamma: Gamma with {}
  given zeta: Zeta with {}
}
