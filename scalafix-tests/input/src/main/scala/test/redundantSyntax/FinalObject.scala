/*
rules = RedundantSyntax
RedundantSyntax.finalObject = true
*/

package test.redundantSyntax

final object FinalObject {
  final object Foo
  private final case object Bar
}

abstract class Class {
  final def bar: String = "bar"
}

@SuppressWarnings(Array("RedundantSyntax"))
final object Suppressed
