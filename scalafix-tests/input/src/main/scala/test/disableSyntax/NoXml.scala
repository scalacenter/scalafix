/*
rules = DisableSyntax
DisableSyntax.noXml = true
 */
package test.disableSyntax

object NoXml {{
  <a>xml</a> /* assert: DisableSyntax.noXml
  ^
  xml literals should be avoided */
}}
