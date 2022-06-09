package test.explicitResultTypes

import java.util.function
// https://github.com/scalacenter/scalafix/issues/1219
object ExplicitResultTypesBackQuoteComplex {
  val it: Iterable[Any] = null.asInstanceOf[Iterable[_]]
  def method(a: String*) = a.head
  val func: Seq[String] => String = method _
  val hash = new java.util.HashMap[String, Int]()
  val func2: (String, function.Function[_ >: String, _ <: Int]) => Int = hash.computeIfAbsent _
}
