package scalafix.internal.config

import scala.meta.io.AbsolutePath
import scala.util.matching.Regex
import metaconfig._

case class FilterMatcher(
    includeFilters: Regex,
    excludeFilters: Regex
) {
  val reader: ConfDecoder[FilterMatcher] =
    ConfDecoder.instanceF[FilterMatcher] {
      case c @ Conf.Str(_) =>
        c.as[Regex].map(FilterMatcher(_, FilterMatcher.mkRegexp(Nil)))
      case c @ Conf.Lst(_) =>
        c.as[List[String]].map { x =>
          FilterMatcher(FilterMatcher.mkRegexp(x), FilterMatcher.mkRegexp(Nil))
        }
      case c =>
        (
          c.getOrElse("include", "includes")(includeFilters) |@|
            c.getOrElse("exclude", "excludes")(excludeFilters)
        ).map { case (a, b) => FilterMatcher(a, b) }
    }
  def matches(file: AbsolutePath): Boolean = matches(file.toString())
  def matches(input: String): Boolean =
    includeFilters.findFirstIn(input).isDefined &&
      excludeFilters.findFirstIn(input).isEmpty
  def unapply(arg: String): Boolean =
    matches(arg)
}

object FilterMatcher {
  lazy val matchEverything: FilterMatcher =
    new FilterMatcher(".*".r, mkRegexp(Nil))
  lazy val matchNothing: FilterMatcher =
    new FilterMatcher(mkRegexp(Nil), mkRegexp(Nil))
  implicit val reader: ConfDecoder[FilterMatcher] = matchEverything.reader

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
