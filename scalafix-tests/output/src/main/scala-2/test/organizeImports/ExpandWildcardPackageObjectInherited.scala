package test.organizeImports

import test.organizeImports.pkgobj.{PlainClass, inheritedMember}

object ExpandWildcardPackageObjectInherited {
  val b = inheritedMember
  val c = new PlainClass
}
