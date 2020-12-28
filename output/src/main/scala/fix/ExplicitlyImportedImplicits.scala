package fix

import fix.Implicits.a.nonImplicit
import fix.Implicits.b._

import scala.concurrent.ExecutionContext

import ExecutionContext.Implicits.global
import fix.Implicits.a.intImplicit
import fix.Implicits.a.stringImplicit

object ExplicitlyImportedImplicits {
  def f1()(implicit i: Int) = ???
  def f2()(implicit s: String) = ???
  f1()
  f2()
}
