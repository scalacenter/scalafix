package scalafix.config

import scala.meta.Tree
import scala.meta.contrib._
import scala.util.matching.Regex

// Hack to compare equality for nested case classes using structural equality
// for scala.meta.Tree fields.
object AnyEqual {
  def assertEqual(x: Any, y: Any): Unit = {
    def loop(x: Any, y: Any): Boolean = {
      val eq = (x, y) match {
        case (x, y) if x == null || y == null => x == null && y == null
        case (Some(x), Some(y)) => loop(x, y)
        case (None, None) => true
        case (x: metaconfig.HasFields, y: metaconfig.HasFields) =>
          loop(x.fields, y.fields)
        case (xs: Iterable[_], ys: Iterable[_]) =>
          xs.toList.length == ys.toList.length &&
            xs.zip(ys).forall { case (x, y) => loop(x, y) }
        case (x: Tree, y: Tree) => x.isEqual(y)
        case (x: Regex, y: Regex) => x.regex == y.regex
        case (x: Product, y: Product) =>
          loop(x.productIterator.toList, y.productIterator.toList)
        case _ =>
          x == y
      }
      if (!eq) throw new IllegalArgumentException(s"""Mismatch!
                                                     |x: ${x.getClass} $x
                                                     |y: ${y.getClass} $y""".stripMargin)
      else true
    }
    loop(x, y)
  }
}
