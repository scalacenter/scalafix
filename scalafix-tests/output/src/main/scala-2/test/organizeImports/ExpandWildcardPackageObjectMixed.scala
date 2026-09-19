package test.organizeImports

import test.organizeImports.pkgobj.{PlainClass, directMember}

object ExpandWildcardPackageObjectMixed {
  val a = directMember
  val c = new PlainClass
}
