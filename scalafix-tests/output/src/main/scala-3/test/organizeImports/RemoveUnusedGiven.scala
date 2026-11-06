package test.organizeImports

object RemoveUnusedGiven {
  object Givens {
    trait T
    given Int = 1
    given s: String = ""
    given t: T = new T {}
    val plain = 1
  }

  object GivenWildcard {
  }

  object GivenImport {
  }

  object GivenImportBesideUsedGiven {
    import Givens.{given String}
    def f(using s: String) = s
    val x = f
  }

  object GivenWildcardBesideName {
  }

  object GivenImportBesideUsedName {
    import Givens.plain
    val x = plain
  }

  object GivenWildcardBesideUsedWildcard {
    import Givens.*
    val x = plain
  }
}
