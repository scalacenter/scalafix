package scalafix.internal.rule

import metaconfig.ConfDecoder
import metaconfig.ConfEncoder
import metaconfig.annotation.Description
import metaconfig.generic

case class RemoveUnusedConfig(
    @Description("Remove unused imports")
    imports: Boolean = true,
    @Description("Remove unused private members")
    privates: Boolean = true,
    @Description("Remove unused local definitions")
    locals: Boolean = true,
    @Description("Remove unused pattern match variables (compatible with Scala 2.12 and 2.13")
    patternvars: Boolean = true
)

object RemoveUnusedConfig {
  val default: RemoveUnusedConfig = RemoveUnusedConfig()
  implicit val surface: generic.Surface[RemoveUnusedConfig] =
    generic.deriveSurface[RemoveUnusedConfig]
  implicit val decoder: ConfDecoder[RemoveUnusedConfig] =
    generic.deriveDecoder[RemoveUnusedConfig](default)
  implicit val encoder: ConfEncoder[RemoveUnusedConfig] =
    generic.deriveEncoder[RemoveUnusedConfig]
}
