/*
rules = [OrganizeImports]
OrganizeImports {
  importsOrder = Ascii
  intellijCompatible = true
}
 */
package fix

import scala.collection.mutable.ArrayBuffer
import fix.Implicits.a._
import scala.concurrent.ExecutionContext.Implicits.global
import fix.Implicits.b.{i, s}

object ExplicitlyImportedImplicitsIntelliJCompatible {
  def f1()(implicit i: Int) = ???
  def f2()(implicit s: String) = ???
  f1()
  f2()
}
