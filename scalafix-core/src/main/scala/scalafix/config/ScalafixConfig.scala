package scalafix
package config

import scala.meta._
import scala.meta.dialects.Scala211
import scala.meta.parsers.Parse

import java.io.File

import metaconfig._
import metaconfig.typesafeconfig.typesafeConfigMetaconfigParser

@DeriveConfDecoder
case class ScalafixConfig(
    parser: Parse[_ <: Tree] = Parse.parseSource,
    @Recurse explicitReturnTypes: ExplicitReturnTypesConfig = ExplicitReturnTypesConfig(),
    @Recurse imports: ImportsConfig = ImportsConfig(),
    @Recurse patches: PatchConfig = PatchConfig(),
    @Recurse debug: DebugConfig = DebugConfig(),
    fatalWarnings: Boolean = true,
    reporter: ScalafixReporter = ScalafixReporter.default,
    dialect: Dialect = Scala211
)

object ScalafixConfig {

  lazy val default = ScalafixConfig()
  implicit lazy val ScalafixConfigDecoder: ConfDecoder[ScalafixConfig] =
    default.reader

  /** Returns config from current working directory, if .scalafix.conf exists. */
  def auto(workingDirectory: AbsolutePath): Option[Input] = {
    val file = workingDirectory.resolve(".scalafix.conf")
    if (file.isFile && file.toFile.exists())
      Some(Input.File(file))
    else None
  }

  def fromInput(input: Input, mirror: Option[Mirror])(
      implicit decoder: ConfDecoder[Rewrite]
  ): Configured[(Rewrite, ScalafixConfig)] =
    typesafeConfigMetaconfigParser
      .fromInput(input)
      .flatMap(config.scalafixConfigConfDecoder.read)
}
