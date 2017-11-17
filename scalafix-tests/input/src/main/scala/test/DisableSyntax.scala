/*
rules = DisableSyntax
DisableSyntax {
  keywords = [
    var
    null
    return
    throw
  ]
  noTabs = true
  noSemicolons = true
  noXml = true
}
*/
package test

case object DisableSyntax {
  var a = 1                 // assert: DisableSyntax.keywords.var
  null                      // assert: DisableSyntax.keywords.null
  def foo: Unit = return    // assert: DisableSyntax.keywords.return
  throw new Exception("ok") // assert: DisableSyntax.keywords.throw

  "semicolon";              // assert: DisableSyntax.noSemicolons
  <a>xml</a>                // assert: DisableSyntax.noXml
	                          // assert: DisableSyntax.noTabs
}
