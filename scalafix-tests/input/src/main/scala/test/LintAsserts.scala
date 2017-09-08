/*
rules = [
  NoInfer
  "class:scalafix.test.DummyLinter"
]
 */
package test

class LintAsserts {
  List(1, (1, 2)) // assert: NoInfer.any
}
// assert: DummyLinter
