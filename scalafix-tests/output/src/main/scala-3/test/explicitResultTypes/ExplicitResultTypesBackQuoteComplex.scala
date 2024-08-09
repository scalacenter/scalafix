package test.explicitResultTypes

// https://github.com/scalacenter/scalafix/issues/1219
object ExplicitResultTypesBackQuoteComplex {
  val it: Iterable[?] = null.asInstanceOf[Iterable[_]]
  def method(a: String*) = a.head
  val func: Seq[String] => String = method _
  val hash = new java.util.HashMap[String, Int]()
  val func2: (String, java.util.function.Function[? >: String <: Object, ? <: Int]) => Int = hash.computeIfAbsent _
}
