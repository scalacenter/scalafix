package scalafix.v1
import scala.meta.io.AbsolutePath

import metaconfig.Conf
import scalafix.internal.config.ScalaVersion

final class Configuration private (
    val scalaVersion: ScalaVersion,
    val scalacOptions: List[String],
    val scalacClasspath: List[AbsolutePath],
    val conf: Conf
) {

  def withScalaVersion(version: ScalaVersion): Configuration = {
    copy(scalaVersion = version)
  }

  def withScalacOptions(options: List[String]): Configuration = {
    copy(scalacOptions = options)
  }

  def withScalacClasspath(classpath: List[AbsolutePath]): Configuration = {
    copy(scalacClasspath = classpath)
  }

  def withConf(conf: Conf): Configuration = {
    copy(conf = conf)
  }

  override def toString: String =
    s"Configuration($scalaVersion, $scalacOptions, $scalacClasspath, $conf)"

  private[this] def copy(
      scalaVersion: ScalaVersion = this.scalaVersion,
      scalacOptions: List[String] = this.scalacOptions,
      scalacClasspath: List[AbsolutePath] = this.scalacClasspath,
      conf: Conf = this.conf
  ): Configuration = {
    new Configuration(
      scalaVersion = scalaVersion,
      scalacOptions = scalacOptions,
      scalacClasspath = scalacClasspath,
      conf = conf
    )
  }
}

object Configuration {
  val runtimeScalaV: ScalaVersion = ScalaVersion
    .from(scala.util.Properties.versionNumberString)
    .toOption
    .getOrElse(ScalaVersion.scala2)

  def apply(): Configuration = {
    new Configuration(
      runtimeScalaV,
      Nil,
      Nil,
      Conf.Obj()
    )
  }
}
