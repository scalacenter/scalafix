package fix

object DottyKeywords {
  // This is enum
  val `enum` = "enum"
  val `inline` = "inline"
  @inline def foo(a: Int) = a
  @scala.inline def buz(a: Int) = a
  object x {
    import DottyKeywords.{`enum` => y}
    println(y)
    class `enum`
    val u: `enum` = new `enum`
  }
  val z = {
    `enum` + 2
  }
  val unchanged = {
    val `enum` = 1
    `enum` + 3
  }
  `enum` + 23
}
