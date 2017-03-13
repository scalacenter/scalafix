package scalafix.config

import scalafix.syntax._
import scala.collection.immutable.Seq
import scala.meta._
import scala.meta.dialects.Scala211
import scala.meta.parsers.Parse
import scalafix.rewrite.ScalafixRewrite
import scalafix.rewrite.ScalafixRewrites
import scalafix.util.FileOps

import java.io.File

import metaconfig.Reader
import metaconfig.hocon.Hocon2Class

@metaconfig.ConfigReader
case class ScalafixConfig(
    rewrites: List[ScalafixRewrite] = ScalafixRewrites.default,
    parser: Parse[_ <: Tree] = Parse.parseSource,
    imports: ImportsConfig = ImportsConfig(),
    patches: PatchConfig = PatchConfig(),
    debug: DebugConfig = DebugConfig(),
    fatalWarnings: Boolean = true,
    dialect: Dialect = Scala211
) {
  implicit val importsConfigReader: Reader[ImportsConfig] = imports.reader
  implicit val patchConfigReader: Reader[PatchConfig] = patches.reader
  implicit val debugConfigReader: Reader[DebugConfig] = debug.reader
}

object ScalafixConfig {

  val default = ScalafixConfig()

  /** Returns config from current working directory, if .scalafix.conf exists. */
  def auto(workingDir: File): ScalafixConfig = {
    val file = new File(workingDir, ".scalafix.conf")
    if (file.isFile && file.exists()) ScalafixConfig.fromFile(file).get
    else ScalafixConfig.default
  }

  def fromFile(file: File): Either[Throwable, ScalafixConfig] =
    fromString(FileOps.readFile(file))

  def fromString(str: String): Either[Throwable, ScalafixConfig] =
    Hocon2Class.gimmeClass[ScalafixConfig](str, default.reader, None)
}
