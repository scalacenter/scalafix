/*
rules = ExplicitResultTypes
ExplicitResultTypes.skipSimpleDefinitions = true
ExplicitResultTypes.fetchScala3CompilerArtifactsOnVersionMismatch = true

// RuleSuite.scala does run this test for expect3_xTarget3_y with x != y
// because it is in a different folder than "explicitResultTypes" - the suffix
// matters. Take that into account if you move this file.

*/
package test.explicitResultTypesRunAcrossMinors

object FetchScala3CompilerArtifactsOnVersionMismatch {
  def foo = 1
}
