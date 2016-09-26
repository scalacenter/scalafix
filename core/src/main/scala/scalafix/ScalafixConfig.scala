package scalafix

import scala.meta.Dialect
import scala.meta.Tree
import scala.meta.dialects.Scala211
import scala.meta.parsers.Parse
import scalafix.rewrite.Rewrite

case class ScalafixConfig(
    rewrites: Seq[Rewrite] = Rewrite.default,
    parser: Parse[_ <: Tree] = Parse.parseSource,
    dialect: Dialect = Scala211
)
