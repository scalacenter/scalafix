package test.organizeImports

// Scala 3 reserved words used as qualifiers must keep their backticks
import test.organizeImports.QuotedIdent.`export`.Other
import test.organizeImports.QuotedIdent.`export`.SimpleSpanProcessor
import test.organizeImports.QuotedIdent.`given`.Other

object Scala3KeywordInScala2
