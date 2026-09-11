package test.organizeImports

import test.organizeImports.qualifiedpkg.QualifiedMember

object ExpandWildcardQualifiedPackage {
  val unqualified: QualifiedMember = new QualifiedMember
  val qualified: qualifiedpkg.QualifiedMember = new qualifiedpkg.QualifiedMember
}