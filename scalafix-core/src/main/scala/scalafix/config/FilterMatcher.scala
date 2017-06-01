package scalafix.config

import scala.meta.io.AbsolutePath
import scala.util.matching.Regex
import scalafix.util.AbsoluteFile
import metaconfig.ExtraName
import metaconfig._

@DeriveConfDecoder
case class FilterMatcher(
    @ExtraName("include") includeFilters: Regex,
    @ExtraName("exclude") excludeFilters: Regex
) {
  def matches(path: AbsolutePath): Boolean = matches(path.toString())
  def matches(file: AbsoluteFile): Boolean = matches(file.path)
  def matches(input: String): Boolean =
    includeFilters.findFirstIn(input).isDefined &&
      excludeFilters.findFirstIn(input).isEmpty
}

object FilterMatcher {
  val matchEverything = new FilterMatcher(".*".r, mkRegexp(Nil))

  def mkRegexp(filters: Seq[String]): Regex =
    filters match {
      case Nil => "$a".r // will never match anything
      case head :: Nil => head.r
      case _ => filters.mkString("(", "|", ")").r
    }

  def apply(includes: Seq[String], excludes: Seq[String]): FilterMatcher =
    new FilterMatcher(mkRegexp(includes), mkRegexp(excludes))
  def apply(include: String): FilterMatcher =
    new FilterMatcher(mkRegexp(Seq(include)), mkRegexp(Nil))
}
