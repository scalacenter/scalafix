/*
rules = ExplicitResultTypes
ExplicitResultTypes.fetchScala3CompilerArtifactsOnVersionMismatch = true
*/
package test.explicitResultTypes

object BeforeScala3_4 {
  def foo = {
    val xs = List(Some(1), None)
    for Some(x) <- xs yield x
  }
}

