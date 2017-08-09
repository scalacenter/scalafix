package scala.meta.internal.scalafix

import scala.meta.Dialect
import scala.meta.Tree
import scala.meta.internal.trees.Origin

object ScalafixScalametaHacks {
  def dialect(language: String): Dialect = Dialect.standards(language)
  def resetOrigin(tree: Tree): Tree = tree.withOrigin(Origin.None)
}
