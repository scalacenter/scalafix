package scalafix.internal.config

import scala.meta._

import metaconfig._
import metaconfig.generic.Surface
import scalafix.Versions
import scalafix.internal.config.ScalafixConfig._

case class ScalafixConfig(
    version: String = Versions.version,
    debug: DebugConfig = DebugConfig(),
    groupImportsByPrefix: Boolean = true,
    fatalWarnings: Boolean = true,
    reporter: ScalafixReporter = ScalafixReporter.default,
    patches: ConfigRulePatches = ConfigRulePatches.default,
    scalaVersion: ScalaVersion = ScalaVersion.scala2,
    sourceScalaVersion: Option[ScalaVersion] = None,
    lint: LintConfig = LintConfig.default
) {
  val dialect = scalaVersion.dialect(sourceScalaVersion)

  def dialectForFile(path: String): Dialect =
    if (path.endsWith(".sbt")) DefaultSbtDialect
    else dialect

  val reader: ConfDecoder[ScalafixConfig] =
    ScalafixConfig.decoder(this)
}

object ScalafixConfig {

  implicit val scalaVersionDecoder: ConfDecoder[ScalaVersion] =
    ConfDecoder.stringConfDecoder.map { version =>
      ScalaVersion.from(version).get
    }
  lazy val default: ScalafixConfig = ScalafixConfig()
  def decoder(default: ScalafixConfig): ConfDecoder[ScalafixConfig] =
    generic.deriveDecoder[ScalafixConfig](default)
  implicit lazy val surface: Surface[ScalafixConfig] =
    generic.deriveSurface[ScalafixConfig]
  implicit lazy val ScalafixConfigDecoder: ConfDecoder[ScalafixConfig] =
    decoder(default)

  val DefaultSbtDialect: Dialect = scala.meta.dialects.Sbt1
}
