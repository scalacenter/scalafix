/*
rules = NoAutoTupling
 */
package test

class NoAutoTupling2 {
  def a(x: (Int, Boolean)) = x
  a(2, true)

  def b(x: Int, y: Boolean) = (x, y)
  b(2, true)

  def c(x: Int, y: Boolean)(z: (String, List[Int])) = (x, y, z)
  c(2, true)("foo", 1 :: 2 :: Nil)

  def d(x: (Int, Boolean))(y: (String, List[Int])) = (x, y)
  d(2, true)("foo", 1 :: 2 :: Nil)
  d(2, true)("foo", 1 :: 2 :: Nil) // scalafix:ok NoAutoTupling

  def e(x: (Int, Boolean))(s: List[String], c: Char)(y: (String, List[Int])) = (x, y)
  e(2, true)("a" :: "b" :: Nil, 'z')("foo", 1 :: 2 :: Nil)

  def f: (((Int, String)) => ((String, List[Int])) => Int) = a => b => a._1
  f(1 + 2, "foo")("bar", 1 :: 2 :: Nil)

  val g = (x: (Int, Boolean)) => x
  g(2, true)

  case class Foo(t: (Int, String))(s: (Boolean, List[Int]))
  // new Foo(1, "foo")(true, Nil)
  Foo(1, "foo")(true, Nil)
  Foo.apply(1, "foo")(true, Nil)

  case class Bar(x: Int, y: String)(s: (Boolean, List[Int]))
  // new Bar(1, "foo")(true, Nil)
  Bar(1, "foo")(true, Nil)
  Bar.apply(1, "foo")(true, Nil)

  object NoFalsePositives {
    def a(a: (Int, Boolean), b: Int) = (a, b)
    a((2, true), 2)

    def b(a: Int, b: Int) = (a, b)
    b(1 + 1, 2)

    case class Foo(x: Int, y: String, z: (Char, Boolean))
    new Foo(42, "foo", ('z', true))
    Foo(42, "foo", ('z', true))
    Foo.apply(42, "foo", ('z', true))
  }

}
