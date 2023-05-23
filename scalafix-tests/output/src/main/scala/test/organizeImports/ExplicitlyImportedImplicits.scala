package test.organizeImports

import test.organizeImports.Implicits.a.nonImplicit
import test.organizeImports.Implicits.b._

import scala.concurrent.ExecutionContext

import ExecutionContext.Implicits.global
import test.organizeImports.Implicits.a.intImplicit
import test.organizeImports.Implicits.a.stringImplicit

object ExplicitlyImportedImplicits {
  def f1()(implicit i: Int) = ???
  def f2()(implicit s: String) = ???
  f1()
  f2()
}
