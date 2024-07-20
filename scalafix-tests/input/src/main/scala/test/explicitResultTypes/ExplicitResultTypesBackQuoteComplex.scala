/*
rules = "ExplicitResultTypes"
*/
package test.explicitResultTypes

// https://github.com/scalacenter/scalafix/issues/1219
object ExplicitResultTypesBackQuoteComplex {
  val it = null.asInstanceOf[Iterable[_]]
  def method(a: String*) = a.head
  val func = method _
  val hash = new java.util.HashMap[String, Int]()
  val func2 = hash.computeIfAbsent _
}
