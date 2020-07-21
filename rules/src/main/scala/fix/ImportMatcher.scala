package fix

import scala.util.matching.Regex

import scala.meta.Importer

sealed trait ImportMatcher {
  def matches(i: Importer): Int
}

object ImportMatcher {
  def parse(pattern: String): ImportMatcher =
    pattern match {
      case p if p startsWith "re:" => ImportMatcher.RE(new Regex(p stripPrefix "re:"))
      case "*"                     => ImportMatcher.Wildcard
      case p                       => ImportMatcher.PlainText(p)
    }

  case class RE(pattern: Regex) extends ImportMatcher {
    override def matches(i: Importer): Int =
      pattern findPrefixMatchOf i.syntax map (_.end) getOrElse 0
  }

  case class PlainText(pattern: String) extends ImportMatcher {
    override def matches(i: Importer): Int = if (i.syntax startsWith pattern) pattern.length else 0
  }

  case object Wildcard extends ImportMatcher {
    // This matcher should not match anything. The wildcard group is always special-cased at the end
    // of the import group matching process.
    def matches(importer: Importer): Int = 0
  }
}
