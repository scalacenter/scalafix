package fix

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global

import fix.Implicits.a._
import fix.Implicits.b.i
import fix.Implicits.b.s

object ExplicitlyImportedImplicitsIntelliJCompatible {
  def f1()(implicit i: Int) = ???
  def f2()(implicit s: String) = ???
  f1()
  f2()
}
