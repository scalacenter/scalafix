package fix

object Implicits {
  object a {
    def nonImplicit: Unit = ???
    implicit def intImplicit: Int = ???
    implicit def stringImplicit: String = ???
  }

  object b {
    implicit def intImplicit: Int = ???
    implicit def stringImplicit: String = ???
  }
}
