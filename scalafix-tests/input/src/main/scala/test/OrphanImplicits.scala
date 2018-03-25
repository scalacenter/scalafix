/*
rules = OrphanImplicits
 */
package test

object OrphanImplicits {
  trait Foo
  case object FooImpl extends Foo
  trait Bar[T]

  object Bar {
    implicit val foo: Foo = ??? // ok
    implicit val foo2 = FooImpl // ok

    implicit val listFoo: List[Foo] = ??? /* assert: OrphanImplicits
    ^
Orphan implicits are not allowed.
This definition is only allowed in one of the following objects:
_root_.scala.package.List#, _root_.test.OrphanImplicits.Foo#
*/
    implicit val listFoo2 = List(FooImpl) // assert: OrphanImplicits

    implicit val either: Either[String, Int] = ??? // ok, because it has 2 type params

    implicit val barFoo: Bar[Foo] = ??? // ok
    implicit def fooFromBarFoo(implicit barFoo: Bar[Foo]): Foo = ??? // ok

    object Foo {
      implicit val barInt: Bar[Int] = ??? // assert: OrphanImplicits
    }
  }

  object Foo {
    implicit val foo: Foo = ??? // ok
    implicit val foo2 = FooImpl
    implicit val listFoo: List[Foo] = ??? // ok
    implicit val listFoo2 = List(FooImpl: Foo) // ok
    // simple List(FooImpl) has List[FooImpl.type] type, thus it causes an error
    implicit val barFoo: Bar[Foo] = ??? // ok
    def fooFromBarFoo(implicit barFoo: Bar[Foo]): Foo = ??? // ok
  }
}
