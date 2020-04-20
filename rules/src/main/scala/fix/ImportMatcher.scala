package fix

import scala.meta.Importer
import scala.util.matching.Regex

sealed trait ImportMatcher {
  def matches(i: Importer): Boolean
}

case class RegexMatcher(pattern: Regex) extends ImportMatcher {
  override def matches(i: Importer): Boolean = (pattern findPrefixMatchOf i.syntax).nonEmpty
}

case class PlainTextMatcher(pattern: String) extends ImportMatcher {
  override def matches(i: Importer): Boolean = i.syntax startsWith pattern
}

case object WildcardMatcher extends ImportMatcher {
  // This matcher should not match anything. The wildcard group is always special-cased at the end
  // of the import group matching process.
  def matches(importer: Importer): Boolean = false
}
