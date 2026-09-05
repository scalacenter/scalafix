package test.explicitResultTypes

object BeforeScala3_4 {
  def foo: List[Int] = {
    val xs = List(Some(1), None)
    for Some(x) <- xs yield x
  }
}