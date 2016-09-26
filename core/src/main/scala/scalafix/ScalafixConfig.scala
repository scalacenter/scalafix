package scalafix

import scala.meta.Dialect
import scalafix.rewrite.Rewrite
import scala.meta.Tree
import scala.meta.dialects.Scala211
import scala.meta.parsers.Parse

case class ScalafixConfig(
    rewrites: Seq[Rewrite] = Rewrite.default,
    parser: Parse[_ <: Tree] = Parse.parseSource,
    dialect: Dialect = Scala211
)
