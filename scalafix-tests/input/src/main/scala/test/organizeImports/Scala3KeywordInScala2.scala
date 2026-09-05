/*
rules = [OrganizeImports]
OrganizeImports.removeUnused = false
OrganizeImports.targetDialect = StandardLayout
*/
package test.organizeImports

// Scala 3 reserved words used as qualifiers must keep their backticks
import test.organizeImports.QuotedIdent.`export`.{SimpleSpanProcessor, Other}
import test.organizeImports.QuotedIdent.`given`.Other

object Scala3KeywordInScala2
