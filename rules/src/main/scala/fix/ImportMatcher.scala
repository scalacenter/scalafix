package fix

import scala.util.matching.Regex

import scala.meta.Importer

sealed trait ImportMatcher {
  def matches(i: Importer): Int
}

case class RegexMatcher(pattern: Regex) extends ImportMatcher {
  override def matches(i: Importer): Int =
    pattern findPrefixMatchOf i.syntax map (_.end) getOrElse 0
}

case class PlainTextMatcher(pattern: String) extends ImportMatcher {
  override def matches(i: Importer): Int = if (i.syntax startsWith pattern) pattern.length else 0
}

case object WildcardMatcher extends ImportMatcher {
  // This matcher should not match anything. The wildcard group is always special-cased at the end
  // of the import group matching process.
  def matches(importer: Importer): Int = 0
}
