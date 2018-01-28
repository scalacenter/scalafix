/*
rules = OrphanImplicits
 */
package test

object OrphanImplicits {
  trait Foo
  trait Bar[T]

  object Bar {
    implicit val foo: Foo = ??? /* assert: OrphanImplicits
    ^
Orphan implicits are not allowed.
You should put this definition to one of the following objects:
_root_.test.OrphanImplicits.Foo#
*/
    implicit val listFoo: List[Foo] = ??? /* assert: OrphanImplicits
    ^
Orphan implicits are not allowed.
You should put this definition to one of the following objects:
_root_.scala.package.List#, _root_.test.OrphanImplicits.Foo#
*/

    implicit val barFoo: Bar[Foo] = ??? // ok

    implicit def fooFromBarFoo(implicit barFoo: Bar[Foo]): Foo = ??? // assert: OrphanImplicits
  }

  object Foo {
    implicit val foo: Foo = ??? // ok
    implicit val listFoo: List[Foo] = ??? // ok
    implicit val barFoo: Bar[Foo] = ??? // ok
    def fooFromBarFoo(implicit barFoo: Bar[Foo]): Foo = ??? // ok
  }
}
