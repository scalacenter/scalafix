package scalafix.sbt

import org.scalameta.BuildInfo
import sbt._, Keys._

/** Command to automatically enable semanticdb-scalac for shell session */
object ScalafixEnable {
  import ScalafixPlugin.autoImport._

  /** sbt 1.0 and 0.13 compatible implementation of partialVersion */
  private def partialVersion(version: String): Option[(Long, Long)] =
    CrossVersion.partialVersion(version).map {
      case (a, b) => (a.toLong, b.toLong)
    }

  lazy val partialToFullScalaVersion: Map[(Long, Long), String] = (for {
    v <- BuildInfo.supportedScalaVersions
    p <- partialVersion(v).toList
  } yield p -> v).toMap

  def projectsWithMatchingScalaVersion(
      state: State): Seq[(ProjectRef, String)] = {
    val extracted = Project.extract(state)
    for {
      p <- extracted.structure.allProjectRefs
      version <- scalaVersion.in(p).get(extracted.structure.data).toList
      partialVersion <- partialVersion(version).toList
      fullVersion <- partialToFullScalaVersion.get(partialVersion).toList
    } yield p -> fullVersion
  }

  lazy val command = Command.command(
    "scalafixEnable",
    briefHelp =
      "Configure libraryDependencies, scalaVersion and scalacOptions for scalafix.",
    detail = """1. enables the semanticdb-scalac compiler plugin
               |2. sets scalaVersion to latest Scala version supported by scalafix
               |3. add -Yrangepos to scalacOptions""".stripMargin
  ) { s =>
    val extracted = Project.extract(s)
    val settings: Seq[Setting[_]] = for {
      (p, fullVersion) <- projectsWithMatchingScalaVersion(s)
      isEnabled = libraryDependencies
        .in(p)
        .get(extracted.structure.data)
        .exists(_.exists(_.name == "semanticdb-scalac"))
      if !isEnabled
      setting <- List(
        scalaVersion.in(p) := fullVersion,
        scalacOptions.in(p) ++= List(
          "-Yrangepos",
          s"-Xplugin-require:semanticdb",
          s"-P:semanticdb:sourceroot:${scalafixSourceroot.value.getAbsolutePath}"
        ),
        libraryDependencies.in(p) += compilerPlugin(
          "org.scalameta" % "semanticdb-scalac" %
            scalafixSemanticdbVersion.value cross CrossVersion.full)
      )
    } yield setting

    val semanticdbInstalled = extracted.append(settings, s)
    semanticdbInstalled
  }
}
