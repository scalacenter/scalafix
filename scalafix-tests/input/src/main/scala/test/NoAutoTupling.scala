/*
rewrites = NoAutoTupling
 */
package test

class NoAutoTupling {
//  <<< add explicit tuple
  object tup {
    def fooo(t: (Int, Int)): Int = ???
    fooo(1 + 1, 2)
  }
//  <<< multiple parameter lists, all auto-tupled
  object tup1 {
    def fooo(t: (Int, Int))(s: (String, String)): Int = ???
    fooo(1, 2)("a", "b")

    def baar(t: (Boolean, Int, String))(s: (Boolean, String))(
        k: (Int, String)): String = ???
    baar(true, 1, "foo")(false, "42")(42, "foo")
  }
//  <<< multiple parameter lists, some auto-tupled
  object tup2 {
    def fooo(t: (Int, Int))(s: (String, String)): Int = ???
    fooo((1, 2))("a", "b")

    def baar(t: (Boolean, Int, String))(s: (Boolean, String))(
        k: (Int, String)): String = ???
    baar(true, 1, "foo")((false, "42"))(42, "foo")
  }
//  <<< already tupled calls stay the same
  object tup3 {
    def fooo(t: (Int, Int)): Int = ???
    fooo((1, 2))
  }
//  <<< methods not involving tuples stay the same
  object tup4 {
    def sum(a: Int, b: Int): Int = ???
    sum(1, 2)
  }
//  <<< methods with tuples, but not single parameter
  object tup5 {
    def sum(a: Int, b: (Int, String)): Int = ???
    sum(1, (2, "foo"))
  }
//  <<< SKIP auto-tupling with lambdas
//  object tup6 {
//    val foo = (a: (Int, Boolean)) => a
//    foo(2, true)
//  }
//  <<< SKIP auto-tupling with curried methods
//  object tup7 {
//    def foo: (((Int, String)) => ((String, List[Int])) => Int) = a => b => a._1
//    foo(1 + 2, "foo")("bar", 1 :: 2 :: Nil)
//  }
//  <<< auto-tupling with class constructors
  object tup8 {
    case class Foo(t: (Int, String))(s: (Boolean, List[Int]))
    new Foo(1, "foo")(true, Nil)
//    Foo(1, "foo")(true, Nil) // blocked by https://github.com/scalameta/scalameta/issues/846
    Foo.apply(1, "foo")(true, Nil)
  }
}
