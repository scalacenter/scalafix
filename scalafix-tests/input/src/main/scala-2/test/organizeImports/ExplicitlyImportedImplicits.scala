/*
rules = [OrganizeImports]
OrganizeImports.removeUnused = false
OrganizeImports.groupExplicitlyImportedImplicitsSeparately = true
 */
package test.organizeImports

import scala.concurrent.ExecutionContext
import test.organizeImports.Implicits.b._
import ExecutionContext.Implicits.global
import test.organizeImports.Implicits.a.{nonImplicit, intImplicit, stringImplicit}

object ExplicitlyImportedImplicits {
  def f1()(implicit i: Int) = ???
  def f2()(implicit s: String) = ???
  f1()
  f2()
}
