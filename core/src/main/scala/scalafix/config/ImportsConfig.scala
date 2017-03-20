package scalafix.config

import scala.collection.immutable.Seq
import scala.meta.Ref
import scala.util.matching.Regex

import metaconfig.String2AnyMap
import metaconfig.Reader

@metaconfig.ConfigReader
case class ImportsConfig(
    // Disabled because unused imports can be created when expanding relative
    // imports, see https://github.com/scalacenter/scalafix/issues/83
    expandRelative: Boolean = false,
    spaceAroundCurlyBrace: Boolean = false,
    organize: Boolean = true,
    removeUnused: Boolean = true,
    alwaysUsed: List[Ref] = List(),
    groups: List[FilterMatcher] = List(
      FilterMatcher("scala.language.*"),
      FilterMatcher("(scala|scala\\..*)$"),
      FilterMatcher("(java|java\\..*)$")
    ),
    groupByPrefix: Boolean = false
) {}

object ImportsConfig {
  def default: ImportsConfig = ImportsConfig()
}
