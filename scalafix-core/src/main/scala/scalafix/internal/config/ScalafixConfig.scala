package scalafix.internal.config

import scala.meta._

import metaconfig._
import metaconfig.generic.Surface
import scalafix.Versions
import scalafix.internal.config.ScalafixConfig._

case class ScalafixConfig(
    version: String = Versions.version,
    //    parser: ParserConfig = ParserConfig(),
    debug: DebugConfig = DebugConfig(),
    groupImportsByPrefix: Boolean = true,
    fatalWarnings: Boolean = true,
    reporter: ScalafixReporter = ScalafixReporter.default,
    patches: ConfigRulePatches = ConfigRulePatches.default,
    dialect: Dialect = ScalafixConfig.DefaultDialect,
    lint: LintConfig = LintConfig.default
) {

  def dialectForFile(path: String): Dialect =
    if (path.endsWith(".sbt")) DefaultSbtDialect
    else dialect

  val reader: ConfDecoder[ScalafixConfig] =
    ScalafixConfig.decoder(this)
}

object ScalafixConfig {

  lazy val default: ScalafixConfig = ScalafixConfig()
  def decoder(default: ScalafixConfig): ConfDecoder[ScalafixConfig] =
    generic.deriveDecoder[ScalafixConfig](default)
  implicit lazy val surface: Surface[ScalafixConfig] =
    generic.deriveSurface[ScalafixConfig]
  implicit lazy val ScalafixConfigDecoder: ConfDecoder[ScalafixConfig] =
    decoder(default)

  val DefaultDialect = scala.meta.dialects.Scala213
  val DefaultSbtDialect = scala.meta.dialects.Sbt1
}
