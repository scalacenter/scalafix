/*
rules = DisableSyntax
DisableSyntax.noWhileLoops = true
DisableSyntax.noDefaultArgs = true
*/
package test.disableSyntax

case object SpecificDisableSyntax {
  class Bar { type Foo = Int; def foo = 42 }

  def foo(a: { type Foo = Int; def foo: Foo } = new Bar): Int = a.foo /* assert: DisableSyntax.defaultArgs
                                                ^^^^^^^
Default args makes it hard to use methods as functions.
*/
  def foo2(a: { type Foo = Int; def foo: Foo } = {val a = new Bar; a}): Int = a.foo /* assert: DisableSyntax.defaultArgs
                                                 ^^^^^^^^^^^^^^^^^^^^
Default args makes it hard to use methods as functions.
*/
  do () while (true) // assert: DisableSyntax.while
}
