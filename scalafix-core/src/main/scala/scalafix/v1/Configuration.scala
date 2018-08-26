package scalafix.v1
import metaconfig.Conf

final class Configuration private (
    val scalaVersion: String,
    val scalacOptions: List[String],
    val conf: Conf
) {

  def withScalaVersion(version: String): Configuration = {
    copy(scalaVersion = version)
  }

  def withScalacOptions(options: List[String]): Configuration = {
    copy(scalacOptions = options)
  }

  def withConf(conf: Conf): Configuration = {
    copy(conf = conf)
  }

  override def toString: String =
    s"Configuration($scalaVersion, $scalacOptions, $conf)"

  private[this] def copy(
      scalaVersion: String = this.scalaVersion,
      scalacOptions: List[String] = this.scalacOptions,
      conf: Conf = this.conf
  ): Configuration = {
    new Configuration(
      scalaVersion = scalaVersion,
      scalacOptions = scalacOptions,
      conf = conf
    )
  }
}

object Configuration {
  def apply(): Configuration = {
    new Configuration(
      scala.util.Properties.versionNumberString,
      Nil,
      Conf.Obj()
    )
  }
}
