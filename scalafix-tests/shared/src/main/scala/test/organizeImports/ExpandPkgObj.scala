package test.organizeImports

trait ExpandPkgObjBase {
  def inheritedMember: Int = 1
}

package object pkgobj extends ExpandPkgObjBase {
  def directMember: Int = 2
}
