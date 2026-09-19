package test.organizeImports

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

  object c {
    implicit def longImplicit: Long = ???
    implicit def floatImplicit: Float = ???
  }

  object d {
    implicit class IntOps(private val self: Int) {
      def incremented: Int = self + 1
    }
  }
}
