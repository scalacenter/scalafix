/*
rules = [OrganizeImports]
OrganizeImports.importsOrder = Ascii
 */
package fix

import scala.concurrent.ExecutionContext
import fix.Implicits.b._
import scala.languageFeature.postfixOps
import ExecutionContext.Implicits.global
import scala.languageFeature.implicitConversions
import fix.Implicits.a.{i, s}

object ExplicitlyImportedImplicitsLanguageFeature {
  def f1()(implicit i: Int) = ???
  def f2()(implicit s: String) = ???
  f1()
  f2()
}
