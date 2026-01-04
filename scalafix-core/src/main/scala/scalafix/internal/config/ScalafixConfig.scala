package scalafix.internal.config

import scala.meta._

import metaconfig._
import metaconfig.generic.Surface
import scalafix.internal.config.ScalafixConfig._

case class ScalafixConfig(
    groupImportsByPrefix: Boolean = true,
    reporter: ScalafixReporter = ScalafixReporter.default,
    patches: ConfigRulePatches = ConfigRulePatches.default,
    scalaVersion: ScalaVersion = ScalaVersion.scala2,
    sourceScalaVersion: Option[ScalaVersion] = None,
    lint: LintConfig = LintConfig.default,
    private val dialectOverride: Map[String, Boolean] = Map.empty
) {
  val dialect: Dialect =
    dialectOverride.foldLeft(scalaVersion.dialect(sourceScalaVersion)) {
      case (cur, (k, v)) if k.nonEmpty =>
        val upper = s"${k.head.toUpper}${k.drop(1)}"
        cur.getClass.getMethods
          .find(method =>
            (
              method.getName == s"with${upper}"
            ) && (
              method.getParameterTypes.toSeq == Seq(classOf[Boolean])
            ) && (
              method.getReturnType == classOf[Dialect]
            )
          )
          .fold(cur)(
            _.invoke(cur, java.lang.Boolean.valueOf(v)).asInstanceOf[Dialect]
          )
      case (cur, _) =>
        cur
    }

  def dialectForFile(path: String): Dialect =
    if (path.endsWith(".sbt")) DefaultSbtDialect
    else if (path.endsWith(".sc"))
      dialect
        .withAllowToplevelTerms(true)
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
