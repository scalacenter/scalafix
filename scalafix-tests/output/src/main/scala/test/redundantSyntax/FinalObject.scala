package test.redundantSyntax

object FinalObject {
  object Foo
  private case object Bar
}

abstract class Class {
  final def bar: String = "bar"
}

@SuppressWarnings(Array("RedundantSyntax"))
final object Suppressed
