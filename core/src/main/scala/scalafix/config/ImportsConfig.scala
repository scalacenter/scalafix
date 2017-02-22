package scalafix.config

import scala.collection.immutable.Seq
import scala.meta.Ref
import scala.util.matching.Regex

import metaconfig.String2AnyMap
import metaconfig.Reader

@metaconfig.ConfigReader
case class ImportsConfig(
    expandRelative: Boolean = true,
    spaceAroundCurlyBrace: Boolean = false,
    organize: Boolean = true,
    removeUnused: Boolean = true,
    alwaysUsed: List[Ref] = List(),
    groups: List[FilterMatcher] = List(
      FilterMatcher("scala.language.*"),
      FilterMatcher("(scala|scala\\..*)$"),
      FilterMatcher("(java|java\\..*)$"),
      FilterMatcher(".*")
    ),
    groupByPrefix: Boolean = false
) {}

object ImportsConfig {
  def default: ImportsConfig = ImportsConfig()
}
