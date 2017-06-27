package scalafix
package config

import java.io.PrintStream
import scala.meta._
import scala.meta.dialects.Scala211
import scala.meta.parsers.Parse
import metaconfig._
//import metaconfig.typesafeconfig.typesafeConfigMetaconfigParser
//import metaconfig.typesafeconfig.typesafeConfigMetaconfigParser
import scalafix.config.MetaconfigParser.parser

@DeriveConfDecoder
case class ScalafixConfig(
    parser: Parse[_ <: Tree] = Parse.parseSource,
    @Recurse explicitReturnTypes: ExplicitReturnTypesConfig =
      ExplicitReturnTypesConfig(),
    @Recurse patches: PatchConfig = PatchConfig(),
    @Recurse debug: DebugConfig = DebugConfig(),
    fatalWarnings: Boolean = true,
    reporter: ScalafixReporter = ScalafixReporter.default,
    dialect: Dialect = Scala211
) {

  def withOut(out: PrintStream): ScalafixConfig = copy(
    reporter = reporter match {
      case r: PrintStreamReporter => r.copy(outStream = out)
      case _ => ScalafixReporter.default.copy(outStream = out)
    }
  )
}

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

  def fromInput(input: Input,
                mirror: LazyMirror,
                extraRewrites: List[String] = Nil)(
      implicit decoder: ConfDecoder[Rewrite]
  ): Configured[(Rewrite, ScalafixConfig)] =
    configFromInput(input, mirror, extraRewrites)

}
