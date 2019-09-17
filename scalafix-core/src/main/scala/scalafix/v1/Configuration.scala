package scalafix.v1
import metaconfig.Conf
import scala.meta.io.AbsolutePath

final class Configuration private (
    val scalaVersion: String,
    val scalacOptions: List[String],
    val scalacClasspath: List[AbsolutePath],
    val conf: Conf
) {

  def withScalaVersion(version: String): Configuration = {
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
      scalaVersion: String = this.scalaVersion,
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
  def apply(): Configuration = {
    new Configuration(
      scala.util.Properties.versionNumberString,
      Nil,
      Nil,
      Conf.Obj()
    )
  }
}
