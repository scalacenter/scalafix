package test.removeUnused

object RemoveUnusedImportsUnimport {
  object UnimportAlone {
  }

  object Unimports {
  }

  object UnimportBesideWildcard {
  }

  object UnimportBesideUsedWildcard {
    import scala.util.*
    val x = Try(1)
  }

  object UnimportBesideUsedName {
    import scala.util.Try
    val x = Try(1)
  }
}
