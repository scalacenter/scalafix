/*
rules = [OrganizeImports]
OrganizeImports.removeUnused = true
OrganizeImports.targetDialect = Scala3
 */
package test.organizeImports

object RemoveUnusedUnimport {
  object UnimportAlone {
    import scala.util.{Random as _}
  }

  object Unimports {
    import scala.util.{Random as _, Left as _}
  }

  object UnimportBesideWildcard {
    import scala.util.{Random as _, *}
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
