/*
rules = [
  Disable
  "class:scalafix.test.NoDummy"
]
Disable.ifSynthetic = [
  "scala.Any"
]
 */
package test

class LintAsserts {
  List(1, (1, 2)) // assert: Disable.Any
  val aDummy = 0 // assert: NoDummy
}

