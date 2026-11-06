/*
rules = RemoveUnused
 */
package test.removeUnused

object RemoveUnusedImportsGiven {
  object Givens {
    trait T
    given Int = 1
    given s: String = ""
    given t: T = new T {}
    val plain = 1
  }

  object GivenWildcard {
    import Givens.given
  }

  object GivenImport {
    import Givens.{given Int}
  }

  object GivenImportBesideUsedGiven {
    import Givens.{given Int, given String}
    def f(using s: String) = s
    val x = f
  }

  object GivenWildcardBesideName {
    import Givens.{plain, given}
  }

  object GivenImportBesideUsedName {
    import Givens.{plain, given Givens.T}
    val x = plain
  }

  object GivenWildcardBesideUsedWildcard {
    import Givens.{*, given}
    val x = plain
  }
}
