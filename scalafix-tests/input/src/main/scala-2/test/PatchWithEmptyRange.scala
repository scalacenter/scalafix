/*
rules = "scala:scalafix.test.PatchTokenWithEmptyRange"
 */
package test

class PatchWithEmptyRange {
  s"${1}${2}"
  <a>{1}{2}</a>
}
