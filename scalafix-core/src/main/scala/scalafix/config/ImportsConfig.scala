package scalafix.config

import scala.collection.immutable.Seq
import scala.meta.Ref
import metaconfig._

@DeriveConfDecoder
case class ImportsConfig(
    // Disabled because unused imports can be created when expanding relative
    // imports, see https://github.com/scalacenter/scalafix/issues/83
    expandRelative: Boolean = false,
    spaceAroundCurlyBrace: Boolean = false,
    // Disabled since users should explicitly opt into it.
    organize: Boolean = false,
    // TODO(olafur) renable when we remove scalafix-nsc.
    removeUnused: Boolean = false,
    alwaysUsed: List[Ref] = List(),
    groups: List[FilterMatcher] = List(
      FilterMatcher("scala.language.*"),
      FilterMatcher("(scala|scala\\..*)$"),
      FilterMatcher("(java|java\\..*)$")
    ),
    groupByPrefix: Boolean = false
)

object ImportsConfig {
  def default: ImportsConfig = ImportsConfig()
}
