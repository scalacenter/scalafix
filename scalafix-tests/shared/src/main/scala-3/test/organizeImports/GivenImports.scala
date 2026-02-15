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

  import scala.collection.mutable.ArrayBuffer
  given alphaArrayBuffer: ArrayBuffer[Alpha] = ???
  given betaArrayBuffer: ArrayBuffer[Beta] = ???
}

object GivenImports2 {
  import GivenImports.*
  import scala.collection.mutable.ArrayBuffer

  given alpha: Alpha = ???
  given beta: Beta = ???
  given gamma: Gamma = ???
  given delta: Delta = ???
  given zeta: Zeta = ???

  given alphaArrayBuffer: ArrayBuffer[Alpha] = ???
  given betaArrayBuffer: ArrayBuffer[Beta] = ???
}
