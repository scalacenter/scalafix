package scalafix.internal

import scala.meta.Tree
import scala.meta.parsers.Parse

package object config extends ScalafixMetaconfigReaders {
  type MetaParser = Parse[_ <: Tree]
}
