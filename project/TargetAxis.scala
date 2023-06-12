import sbt._
import sbt.Keys._
import sbt.internal.ProjectMatrix
import sbtprojectmatrix.ProjectMatrixPlugin.autoImport._
import scala.reflect.ClassTag

/**
 * Use on ProjectMatrix rows to tag an affinity to a project with a custom
 * scalaVersion, with or without `-Xsource:3`
 */
case class TargetAxis(scalaVersion: String, xsource3: Boolean = false)
    extends VirtualAxis.WeakAxis {

  private val maybeSource3 = if (xsource3) "xsource3" else ""

  override val idSuffix =
    s"Target${scalaVersion.replace('.', '_')}$maybeSource3"
  override val directorySuffix = s"target$scalaVersion$maybeSource3"
  override val suffixOrder = VirtualAxis.scalaABIVersion("any").suffixOrder + 1

  /** Axis values the targeted project should have */
  private def axisValues: Seq[VirtualAxis] =
    VirtualAxis.ScalaVersionAxis(scalaVersion, scalaVersion) +:
      (if (xsource3) Seq(Xsource3Axis) else Seq())

  /** Settings the targeted project should have */
  private def settings: Seq[Setting[_]] =
    if (xsource3)
      Seq(
        scalacOptions += "-Xsource:3",
        Compile / unmanagedSourceDirectories ~= {
          _.map { dir => file(s"${dir.getAbsolutePath}-xsource3") }
        }
      )
    else Seq()
}

/** Use on ProjectMatrix rows to mark usage of "-Xsource:3" */
case object Xsource3Axis extends VirtualAxis.WeakAxis {
  override val idSuffix = "xsource3"
  override val directorySuffix = idSuffix
  override val suffixOrder = VirtualAxis.scalaABIVersion("any").suffixOrder + 2
}

object TargetAxis {

  /**
   * Lookup the project with axes best matching the TargetAxis, and resolve
   * `key`
   */
  def resolve[T](
      matrix: ProjectMatrix,
      key: TaskKey[T]
  ): Def.Initialize[Task[T]] =
    Def.taskDyn {
      val project = lookup(matrix, virtualAxes.value)
      Def.task((project / key).value)
    }

  /**
   * Lookup the project with axes best matching the TargetAxis, and resolve
   * `key`
   */
  def resolve[T](
      matrix: ProjectMatrix,
      key: SettingKey[T]
  ): Def.Initialize[T] =
    Def.settingDyn {
      val project = lookup(matrix, virtualAxes.value)
      Def.setting((project / key).value)
    }

  /**
   * Find the project best matching the TargetAxis requests present in the
   * provided virtualAxes:
   *   - presence or absence of the `-Xsource:3` flag
   *   - full Scala version when available or a binary one otherwise
   */
  private def lookup(
      matrix: ProjectMatrix,
      virtualAxes: Seq[VirtualAxis]
  ): Project = {
    val TargetAxis(scalaVersion, xsource3) =
      virtualAxes.collectFirst { case x: TargetAxis => x }.get

    val projects = matrix
      .allProjects()
      .collect {
        case (project, projectVirtualAxes)
            if projectVirtualAxes.contains(Xsource3Axis) == xsource3 =>
          (
            projectVirtualAxes
              .collectFirst { case x: VirtualAxis.ScalaVersionAxis => x }
              .get
              .value,
            project
          )
      }
      .toMap

    val fullMatch = projects.get(scalaVersion)

    def binaryMatch = {
      val scalaBinaryVersion = CrossVersion.binaryScalaVersion(scalaVersion)
      projects.find(_._1.startsWith(scalaBinaryVersion)).map(_._2)
    }

    fullMatch.orElse(binaryMatch).get
  }

  implicit class TargetProjectMatrix(projectMatrix: ProjectMatrix) {

    /**
     * Create one JVM project for each target, tagged and configured with the
     * requests of that target
     */
    def jvmPlatformTargets(targets: Seq[TargetAxis]): ProjectMatrix = {
      targets.foldLeft(projectMatrix) { (acc, target) =>
        acc.customRow(
          autoScalaLibrary = true,
          axisValues = target.axisValues :+ VirtualAxis.jvm,
          process = { _.settings(target.settings) }
        )
      }
    }

    /**
     * Create one JVM project for each tuple (Scala version, affinity)
     */
    def jvmPlatformAgainstTargets(
        scalaVersionAgainstTarget: Seq[(String, TargetAxis)]
    ): ProjectMatrix = {
      scalaVersionAgainstTarget.foldLeft(projectMatrix) {
        case (acc, (scalaVersion, target)) =>
          acc.jvmPlatform(
            scalaVersions = Seq(scalaVersion),
            axisValues = Seq(target),
            settings = Seq()
          )
      }
    }

  }

}
