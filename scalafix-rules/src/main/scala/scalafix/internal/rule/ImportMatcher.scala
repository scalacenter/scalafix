package scalafix.internal.rule

import scala.util.matching.Regex

import scala.meta.Importer
import scala.meta.XtensionSyntax

sealed trait ImportMatcher {
  def matches(i: Importer): Int
}

object ImportMatcher {
  def parse(pattern: String): ImportMatcher =
    pattern match {
      case p if p startsWith "re:" => RE(new Regex(p stripPrefix "re:"))
      case "---" => ---
      case "*" => *
      case p => PlainText(p)
    }

  case class RE(pattern: Regex) extends ImportMatcher {
    override def matches(i: Importer): Int =
      pattern findPrefixMatchOf i.syntax map (_.end) getOrElse 0
  }

  case class PlainText(pattern: String) extends ImportMatcher {
    override def matches(i: Importer): Int =
      if (i.syntax startsWith pattern) pattern.length else 0
  }

  case object * extends ImportMatcher {
    // The wildcard matcher matches nothing. It is special-cased at the end of the import group
    // matching process.
    def matches(importer: Importer): Int = 0
  }

  case object --- extends ImportMatcher {
    // Blank line matchers are pseudo matchers matching nothing. They are special-cased at the end
    // of the import group matching process.
    override def matches(i: Importer): Int = 0
  }
}
