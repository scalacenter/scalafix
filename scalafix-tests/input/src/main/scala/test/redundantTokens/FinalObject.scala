/*
rules = RedundantTokens
RedundantTokens.finalObject = true
*/

package test.redundantTokens

final object FinalObject {
  final object Foo
  private final case object Bar
}