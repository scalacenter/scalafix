package scalafix.internal.config

import scala.util.Failure
import scala.util.Success
import scala.util.Try

import scala.meta.Dialect

import scalafix.internal.config.ScalaVersion._

/*
  ScalaVersion allows to model major, minor and patch versions. The expected input is x[.x[.x]]].
  All information stored after a dash are dropped. Example 3.0.0-RC3 will be modeled as ScalaVersion.Patch(3, 0, 0)
 */
sealed trait ScalaVersion {
  val major: MajorVersion
  val minor: Option[Int]
  val patch: Option[Int]
  val dialect: Dialect = major match {
    case ScalaVersion.Major.Scala2 => scala.meta.dialects.Scala213
    case ScalaVersion.Major.Scala3 => scala.meta.dialects.Scala3
  }

  def isScala2: Boolean = major == Major.Scala2
  def isScala3: Boolean = major == Major.Scala3

  def value: String = this match {
    case Major(major) => s"${major.value}"
    case Minor(major, minorVersion) => s"${major.value}.${minorVersion}"
    case Patch(major, minorVersion, patchVersion) =>
      s"${major.value}.${minorVersion}.$patchVersion"
  }
}

object ScalaVersion {
  val scala2: Major = Major(Major.Scala2)
  val scala3: Major = Major(Major.Scala3)

  case class Major(major: MajorVersion) extends ScalaVersion {
    override val minor = None
    override val patch = None
  }
  case class Minor(major: MajorVersion, minorVersion: Int)
      extends ScalaVersion {
    override val minor: Some[Int] = Some(minorVersion)
    override val patch = None

  }
  case class Patch(major: MajorVersion, minorVersion: Int, patchVersion: Int)
      extends ScalaVersion {
    override val minor: Some[Int] = Some(minorVersion)
    override val patch: Some[Int] = Some(patchVersion)
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
  private val FullVersion =
    raw"""($intPattern)\.($intPattern)\.($intPattern)""".r
  private val MajorPattern = raw"""($intPattern)""".r
  private val PartialVersion = raw"""($intPattern)\.($intPattern)""".r

  def from(s: String): Try[ScalaVersion] = {
    val version = s.split("-").head
    version match {
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
      case _ => Failure(new Exception(s"$s not a valid Scala Version."))
    }
  }
}
