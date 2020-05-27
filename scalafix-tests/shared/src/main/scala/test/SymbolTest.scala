package test

import a.`b.c`.d

object a {
  object `b.c` {
    object d
  }
}
trait SymbolTest {
  def shouldBe(right: Any): Unit
  def arg = 1
  this shouldBe (arg)
}
