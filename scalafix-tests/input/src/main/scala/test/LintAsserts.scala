/*
rules = [
  NoInfer
  "class:scalafix.test.NoDummy"
]
 */
package test

class LintAsserts {
  List(1, (1, 2)) // assert: NoInfer.any
  val aDummy = 0 // assert: NoDummy
}

