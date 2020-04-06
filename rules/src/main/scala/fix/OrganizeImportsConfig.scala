package fix

import metaconfig.generic.{deriveDecoder, deriveEncoder, deriveSurface, Surface}
import metaconfig.{ConfDecoder, ConfEncoder}

final case class OrganizeImportsConfig(
  sortImportees: Boolean = false,
  groups: Seq[String] = Seq("*")
)

object OrganizeImportsConfig {
  val default: OrganizeImportsConfig = OrganizeImportsConfig()

  implicit val surface: Surface[OrganizeImportsConfig] =
    deriveSurface[OrganizeImportsConfig]

  implicit val decoder: ConfDecoder[OrganizeImportsConfig] =
    deriveDecoder[OrganizeImportsConfig](default)

  implicit val encoder: ConfEncoder[OrganizeImportsConfig] =
    deriveEncoder[OrganizeImportsConfig]
}
