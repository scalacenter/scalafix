package fix

import scala.concurrent.ExecutionContext
import scala.languageFeature.implicitConversions
import scala.languageFeature.postfixOps

import fix.Implicits.b._

import ExecutionContext.Implicits.global
import fix.Implicits.a.i
import fix.Implicits.a.s

object ExplicitlyImportedImplicitsLanguageFeature {
  def f1()(implicit i: Int) = ???
  def f2()(implicit s: String) = ???
  f1()
  f2()
}
