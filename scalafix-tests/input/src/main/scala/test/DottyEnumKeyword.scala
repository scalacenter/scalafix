/*
rewrite = DottyEnumKeyword
 */
package fix

object DottyEnumKeyword {
  // This is enum
  val enum = "enum"
  object x {
    import DottyEnumKeyword.{enum => y}
    println(y)
    class enum
    val u: enum = new enum
  }
  val z = {
    enum + 2
  }
  val unchanged = {
    val `enum` = 1
    `enum` + 3
  }
  import DottyEnumKeyword.{z => enum}
  enum + 23
}
