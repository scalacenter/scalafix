/*
rules = [
  Disable
  "class:scalafix.test.NoDummy"
]
 */
package test

class LintAsserts {
  val aDummy = 0 // assert: NoDummy
}

