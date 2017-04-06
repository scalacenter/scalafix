package scalafix.config

import scala.collection.immutable.Seq
import scala.meta._
import scala.meta.dialects.Scala211
import scala.meta.parsers.Parse
import scalafix.rewrite.ScalafixRewrite
import scalafix.syntax._
import scalafix.util.FileOps

import java.io.File

import metaconfig.Conf
import metaconfig.ConfDecoder
import metaconfig.typesafeconfig.TypesafeConfig2Class
import metaconfig.Recurse
import metaconfig.Result

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
  implicit val ScalafixConfigDecoder: ConfDecoder[ScalafixConfig] =
    default.reader

  /** Returns config from current working directory, if .scalafix.conf exists. */
  def auto(workingDir: File): Option[ScalafixConfig] = {
    val file = new File(workingDir, ".scalafix.conf")
    if (file.isFile && file.exists()) Some(ScalafixConfig.fromFile(file).get)
    else None
  }

  private def gimmeClass[T](conf: Result[Conf])(
      implicit reader: metaconfig.ConfDecoder[T]): metaconfig.Result[T] =
    for {
      config <- conf.right
      cls <- reader.read(config).right
    } yield cls

  def fromFile(file: File): Either[Throwable, ScalafixConfig] =
    gimmeClass[ScalafixConfig](TypesafeConfig2Class.gimmeConfFromFile(file))

  def fromString(str: String): Either[Throwable, ScalafixConfig] =
    gimmeClass[ScalafixConfig](TypesafeConfig2Class.gimmeConfFromString(str))
}
