package scalafix.internal.rule

import metaconfig.annotation.Description
import metaconfig.generic

case class RemoveUnusedConfig(
    @Description("Remove unused imports")
    imports: Boolean = true,
    @Description("Remove unused private members")
    privates: Boolean = true,
    @Description("Remove unused local definitions")
    locals: Boolean = true
)

object RemoveUnusedConfig {
  val default = RemoveUnusedConfig()
  implicit val surface = generic.deriveSurface[RemoveUnusedConfig]
  implicit val decoder = generic.deriveDecoder[RemoveUnusedConfig](default)
  implicit val encoder = generic.deriveEncoder[RemoveUnusedConfig]
}