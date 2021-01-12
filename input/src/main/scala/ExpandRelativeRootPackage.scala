/*
rules = [OrganizeImports]
OrganizeImports {
  expandRelative = true
  groupedImports = Explode
}
 */
import P._
import Q.x
import Q._

object P {
  object x
}

object Q {
  object x
  object y
}

object ExpandRelativeRootPackage
