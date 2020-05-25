/*
rules = [OrganizeImports]
OrganizeImports {
  importsOrder = Ascii
  groupExplicitlyImportedImplicitsSeparately = true
}
 */
package fix

import scala.concurrent.ExecutionContext
import fix.Implicits.b._
import ExecutionContext.Implicits.global
import fix.Implicits.a.{i, s}

object ExplicitlyImportedImplicits {
  def f1()(implicit i: Int) = ???
  def f2()(implicit s: String) = ???
  f1()
  f2()
}
