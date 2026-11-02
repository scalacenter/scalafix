package test.removeUnused

object RemoveUnusedImportsUnimport {
  object UnimportAlone {
  }

  object Unimports {
  }

  object UnimportBesideWildcard {
    import scala.util.{Random as _}
  }

  object UnimportBesideUsedWildcard {
    import scala.util.{Random as _, *}
    val x = Try(1)
  }

  object UnimportBesideUsedName {
    import scala.util.{Random as _, Try}
    val x = Try(1)
  }
}
