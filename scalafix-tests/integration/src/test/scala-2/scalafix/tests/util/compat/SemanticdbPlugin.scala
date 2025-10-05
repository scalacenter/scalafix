package scalafix.tests.util.compat

import java.nio.file.Paths

import buildinfo.RulesBuildInfo.scalaVersion
import coursier.Dependency
import coursier.Fetch
import coursier.Module
import coursier.ModuleName
import coursier.Organization
import coursier.maven.MavenRepository
import scalafix.tests.BuildInfo

object SemanticdbPlugin {
  def semanticdbPluginPath(): String = {
    val dep = Dependency(
      Module(
        Organization("org.scalameta"),
        ModuleName(s"semanticdb-scalac_$scalaVersion")
      ),
      BuildInfo.scalametaVersion
    )
    val paths = Fetch()
      .addDependencies(dep)
      .addRepositories(
        MavenRepository(
          "https://central.sonatype.com/repository/maven-snapshots"
        )
      )
      .run()
    val semanticdbscalac = paths.collectFirst {
      case path if path.toString.contains("semanticdb-scalac_") =>
        Paths.get(path.toURI).toString
    }
    semanticdbscalac.getOrElse {
      throw new IllegalStateException(
        "unable to auto-detect semanticdb-scalac compiler plugin"
      )
    }
  }
}
