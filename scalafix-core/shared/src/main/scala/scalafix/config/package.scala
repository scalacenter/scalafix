package scalafix

import scala.meta.Tree
import scala.meta.parsers.Parse
import scala.meta.semanticdb.Database

package object config extends ScalafixMetaconfigReaders {
  type MetaParser = Parse[_ <: Tree]
  // The challenge when loading a rewrite is that 1) if it's semantic it needs a
  // mirror constructor argument and 2) we don't know upfront if it's semantic.
  // For example, to know if a classloaded rewrites is semantic or syntactic
  // we have to test against it's Class[_]. For default rewrites, the interface
  // to detect if a rewrite is semantic is different.
  // LazyMirror allows us to delay the computation of a mirror right up until
  // the moment we instantiate the rewrite.
  type LazyMirror = RewriteKind => Option[Database]
}
