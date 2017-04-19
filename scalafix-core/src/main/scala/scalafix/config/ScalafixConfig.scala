package scalafix
package config

import scala.collection.immutable.Seq
import scala.meta._
import scala.meta.dialects.Scala211
import scala.meta.parsers.Parse

import java.io.File

import metaconfig._
import metaconfig.typesafeconfig.TypesafeConfig2Class

@DeriveConfDecoder
case class ScalafixConfig(
//    rewrites: List[SyntaxRewrite] = Nil,
    parser: Parse[_ <: Tree] = Parse.parseSource,
    @Recurse imports: ImportsConfig = ImportsConfig(),
    @Recurse patches: PatchConfig = PatchConfig(),
    @Recurse debug: DebugConfig = DebugConfig(),
    fatalWarnings: Boolean = true,
    reporter: ScalafixReporter = ScalafixReporter.default,
    dialect: Dialect = Scala211
) {

//  def withRewrites[B](
//      f: List[SyntaxRewrite] => List[SyntaxRewrite]): ScalafixConfig =
//    copy(rewrites = f(rewrites).distinct)
}

object ScalafixConfig {

  val default = ScalafixConfig()
  implicit val ScalafixConfigDecoder: ConfDecoder[ScalafixConfig] =
    default.reader

  /** Returns config from current working directory, if .scalafix.conf exists. */
  def auto(workingDir: File): Option[ScalafixConfig] = {
    val file = new File(workingDir, ".scalafix.conf")
    if (file.isFile && file.exists()) Some(ScalafixConfig.fromFile(file).get)
    else None
  }

  private def gimmeClass[T](conf: Configured[Conf])(
      implicit reader: metaconfig.ConfDecoder[T]): metaconfig.Configured[T] =
    for {
      config <- conf
      cls <- reader.read(config)
    } yield cls

  def fromFile(file: File): Configured[ScalafixConfig] =
    gimmeClass[ScalafixConfig](TypesafeConfig2Class.gimmeConfFromFile(file))

  def fromString(str: String): Configured[ScalafixConfig] =
    gimmeClass[ScalafixConfig](TypesafeConfig2Class.gimmeConfFromString(str))
}
