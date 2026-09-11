package test.organizeImports

import test.organizeImports.pkgobj.{directMember, inheritedMember}

object ExpandWildcardPackageObject {
  val a = directMember
  val b = inheritedMember
}
