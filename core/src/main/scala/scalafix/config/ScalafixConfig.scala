package scalafix.config

import scala.meta.Dialect
import scala.meta.Tree
import scala.meta.dialects.Scala211
import scala.meta.parsers.Parse
import scalafix.rewrite.Rewrite

import java.io.File

case class ScalafixConfig(
    rewrites: Seq[Rewrite] = Rewrite.default,
    parser: Parse[_ <: Tree] = Parse.parseSource,
    dialect: Dialect = Scala211,
    project: ProjectFiles = ProjectFiles()
)
