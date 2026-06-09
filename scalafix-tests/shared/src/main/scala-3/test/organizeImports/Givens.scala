package test.organizeImports

object ZGivens { // sorted after Givens
  trait AA[T]
  trait EE[T]
}

object Givens {
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
  given aa_a: ZGivens.AA[A] = ???
  given ee_b: ZGivens.EE[B] = ???
}

object AGivens { // sorted before Givens
  given a: Givens.A = ???
  given b: Givens.B = ???
}
