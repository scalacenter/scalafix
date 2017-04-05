package scalafix.config

import scala.collection.immutable.Seq
import scala.meta._
import scala.meta.dialects.Scala211
import scala.meta.parsers.Parse
import scalafix.rewrite.ScalafixRewrite
import scalafix.syntax._
import scalafix.util.FileOps

import java.io.File

import metaconfig.hocon.Hocon2Class
import metaconfig.Recurse

@metaconfig.DeriveConfDecoder
case class ScalafixConfig(
    rewrites: List[ScalafixRewrite] = Nil,
    parser: Parse[_ <: Tree] = Parse.parseSource,
    @Recurse imports: ImportsConfig = ImportsConfig(),
    @Recurse patches: PatchConfig = PatchConfig(),
    @Recurse debug: DebugConfig = DebugConfig(),
    fatalWarnings: Boolean = true,
    dialect: Dialect = Scala211
) {
  def withRewrites(
      f: List[ScalafixRewrite] => List[ScalafixRewrite]): ScalafixConfig =
    copy(rewrites = f(rewrites).distinct)
}

object ScalafixConfig {

  val default = ScalafixConfig()

  /** Returns config from current working directory, if .scalafix.conf exists. */
  def auto(workingDir: File): Option[ScalafixConfig] = {
    val file = new File(workingDir, ".scalafix.conf")
    if (file.isFile && file.exists()) Some(ScalafixConfig.fromFile(file).get)
    else None
  }

  def fromFile(file: File): Either[Throwable, ScalafixConfig] =
    fromString(FileOps.readFile(file))

  def fromString(str: String): Either[Throwable, ScalafixConfig] =
    Hocon2Class.gimmeClass[ScalafixConfig](str, default.reader, None)
}
