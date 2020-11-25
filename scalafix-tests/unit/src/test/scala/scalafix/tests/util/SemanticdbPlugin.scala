package scalafix.tests.util

import java.nio.file.Paths

import buildinfo.RulesBuildInfo.scalaVersion
import coursier.Dependency
import coursier.Fetch
import coursier.Module
import coursier.ModuleName
import coursier.Organization

object SemanticdbPlugin {
  def semanticdbPluginPath(): String = {
    val dep = Dependency(
      Module(
        Organization("org.scalameta"),
        ModuleName(s"semanticdb-scalac_$scalaVersion")
      ),
      "4.4.0"
    )
    val paths = Fetch()
      .addDependencies(dep)
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
