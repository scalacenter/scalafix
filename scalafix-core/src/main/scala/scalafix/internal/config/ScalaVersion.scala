package scalafix.internal.config

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import scala.util.Failure
import scala.util.Success
import scala.util.Try

import scala.meta.Dialect
import scala.meta.dialects

import scalafix.internal.config.ScalaVersion._

sealed trait ScalaVersion {
  val major: MajorVersion
  val minor: Option[Int]
  val patch: Option[Int]
  val rc: Option[Int]
  val shortSha1: Option[String]

  def binary: Try[ScalaVersion] = (major, minor) match {
    case (Major.Scala2, None) =>
      Failure(
        new Exception(
          s"Minor version not found but required for Scala 2 binary version"
        )
      )
    case (Major.Scala2, Some(minor)) =>
      Success(Minor(major, minor))
    case (_, _) =>
      Success(Major(major))
  }

  def dialect(sourceScalaVersion: Option[ScalaVersion]): Dialect =
    (major, sourceScalaVersion.map(_.major)) match {
      case (Major.Scala3, _) => dialects.Scala3
      case (Major.Scala2, Some(Major.Scala3)) => dialects.Scala213Source3
      case (Major.Scala2, _) => dialects.Scala213
    }

  def isScala2: Boolean = major == Major.Scala2
  def isScala3: Boolean = major == Major.Scala3

  def value: String = this match {
    case Major(major) => s"${major.value}"
    case Minor(major, minorVersion) => s"${major.value}.${minorVersion}"
    case Patch(major, minorVersion, patchVersion) =>
      s"${major.value}.${minorVersion}.$patchVersion"
    case RC(major, minorVersion, patchVersion, rcVersion) =>
      s"${major.value}.${minorVersion}.$patchVersion-RC$rcVersion"
    // Scala 3 nightly
    case Nightly(
          major,
          minorVersion,
          patchVersion,
          maybeRcVersion,
          Some(dateVersion),
          shortSha1
        ) =>
      val maybeRcSuffix = maybeRcVersion.map("-RC" + _).getOrElse("")
      s"${major.value}.${minorVersion}.$patchVersion$maybeRcSuffix-bin-${dateVersion.format(yyyyMMdd)}-$shortSha1-NIGHTLY"
    // Scala 2 nightly
    case Nightly(major, minorVersion, patchVersion, _, _, shortSha1) =>
      s"${major.value}.${minorVersion}.$patchVersion-bin-$shortSha1"
  }
}

object ScalaVersion {
  val scala2: Major = Major(Major.Scala2)
  val scala3: Major = Major(Major.Scala3)

  case class Major(major: MajorVersion) extends ScalaVersion {
    override val minor = None
    override val patch = None
    override val rc = None
    override val shortSha1 = None
  }
  case class Minor(major: MajorVersion, minorVersion: Int)
      extends ScalaVersion {
    override val minor: Some[Int] = Some(minorVersion)
    override val patch = None
    override val rc = None
    override val shortSha1 = None
  }
  case class Patch(major: MajorVersion, minorVersion: Int, patchVersion: Int)
      extends ScalaVersion {
    override val minor: Some[Int] = Some(minorVersion)
    override val patch: Some[Int] = Some(patchVersion)
    override val rc = None
    override val shortSha1 = None
  }

  case class RC(
      major: MajorVersion,
      minorVersion: Int,
      patchVersion: Int,
      rcVersion: Int
  ) extends ScalaVersion {
    override val minor: Some[Int] = Some(minorVersion)
    override val patch: Some[Int] = Some(patchVersion)
    override val rc: Some[Int] = Some(rcVersion)
    override val shortSha1 = None
  }

  case class Nightly(
      major: MajorVersion,
      minorVersion: Int,
      patchVersion: Int,
      rcVersion: Option[Int],
      dateVersion: Option[LocalDate],
      shortSha1Version: String
  ) extends ScalaVersion {
    override val minor: Some[Int] = Some(minorVersion)
    override val patch: Some[Int] = Some(patchVersion)
    override val rc = None
    override val shortSha1: Some[String] = Some(shortSha1Version)
  }

  sealed trait MajorVersion {
    val value: Int
  }
  object Major {
    case object Scala2 extends MajorVersion {
      override val value: Int = 2
    }

    case object Scala3 extends MajorVersion {
      override val value: Int = 3
    }
  }

  object MajorVersion {
    def from(int: Long): Try[MajorVersion] =
      int match {
        case 2 => Success(Major.Scala2)
        case 3 => Success(Major.Scala3)
        case _ =>
          Failure(
            new Exception(s"Major Scala version can be either 2 or 3 not $int")
          )
      }
  }

  private val intPattern = """\d{1,2}"""
  private val datePattern = """\d{8}"""
  private val shortSha1Pattern = """[0-9a-f]{7}"""

  private val FullVersion =
    raw"""($intPattern)\.($intPattern)\.($intPattern)""".r
  private val RcVersion =
    raw"""($intPattern)\.($intPattern)\.($intPattern)-RC($intPattern)""".r
  private val NightlyVersion =
    raw"""($intPattern)\.($intPattern)\.($intPattern)(?:-RC($intPattern))?-bin(?:-($datePattern))?-($shortSha1Pattern)(?:-NIGHTLY)?""".r
  private val MajorPattern = raw"""($intPattern)""".r
  private val PartialVersion = raw"""($intPattern)\.($intPattern)""".r

  private val yyyyMMdd = DateTimeFormatter.ofPattern("yyyyMMdd")

  def from(version: String): Try[ScalaVersion] = {
    version match {
      case NightlyVersion(major, minor, patch, rc, date, shortSha1) =>
        MajorVersion.from(major.toLong).flatMap { major =>
          Success(
            Nightly(
              major,
              minor.toInt,
              patch.toInt,
              Try(rc.toInt).toOption,
              Try(LocalDate.parse(date, yyyyMMdd)).toOption,
              shortSha1
            )
          )
        }
      case RcVersion(major, minor, patch, rc) =>
        MajorVersion.from(major.toLong).flatMap { major =>
          Success(RC(major, minor.toInt, patch.toInt, rc.toInt))
        }
      case FullVersion(major, minor, patch) =>
        MajorVersion.from(major.toLong).flatMap { major =>
          Success(Patch(major, minor.toInt, patch.toInt))
        }
      case PartialVersion(major, minor) =>
        MajorVersion.from(major.toLong).flatMap { major =>
          Success(Minor(major, minor.toInt))
        }
      case MajorPattern(major) =>
        MajorVersion.from(major.toLong).flatMap(major => Success(Major(major)))
      case _ => Failure(new Exception(s"$version not a valid Scala Version."))
    }
  }
}
