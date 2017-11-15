/*
rules = DisableSyntax
DisableSyntax {
  keywords = [
    var
    null
    return
    throw
  ]
  tab = true
  semicolon = true
  xml = true
}

*/
package test

case object DisableSyntax {
  var a = 1                 // assert: DisableSyntax.var
  null                      // assert: DisableSyntax.null
  def foo: Unit = return    // assert: DisableSyntax.return
  throw new Exception("ok") // assert: DisableSyntax.throw

  "semicolon";              // assert: DisableSyntax.semicolon
  <a>xml</a>                // assert: DisableSyntax.xml
}