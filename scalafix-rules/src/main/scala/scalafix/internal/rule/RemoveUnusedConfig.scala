package scalafix.internal.rule

import metaconfig.annotation.Description
import metaconfig.generic
import metaconfig.{ ConfDecoder, ConfEncoder }

case class RemoveUnusedConfig(
    @Description("Remove unused imports")
    imports: Boolean = true,
    @Description("Remove unused private members")
    privates: Boolean = true,
    @Description("Remove unused local definitions")
    locals: Boolean = true
)

object RemoveUnusedConfig {
  val default: RemoveUnusedConfig = RemoveUnusedConfig()
  implicit val surface: generic.Surface[RemoveUnusedConfig] = generic.deriveSurface[RemoveUnusedConfig]
  implicit val decoder: ConfDecoder[RemoveUnusedConfig] = generic.deriveDecoder[RemoveUnusedConfig](default)
  implicit val encoder: ConfEncoder[RemoveUnusedConfig] = generic.deriveEncoder[RemoveUnusedConfig]
}
