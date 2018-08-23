package scalafix.internal.config
import metaconfig.generic

case class Optimization(
    // If enabled, scalafix will not parse a source file unless a rule calls `.tree`.
    // The downside from enabling this feature is that Scalafix will not be able to
    // report "unused scalafix suppression" errors for source files that empty patches.
    skipParsingWhenPossible: Boolean = false
)

object Optimization {
  def default = Optimization()
  implicit val surface = generic.deriveSurface[Optimization]
  implicit val decoder = generic.deriveDecoder[Optimization](default)
  implicit val encoder = generic.deriveEncoder[Optimization]
}
