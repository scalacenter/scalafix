package docs

import java.nio.file.Paths
import scalafix.Versions

object Main {

  def main(args: Array[String]): Unit = {
    val settings = mdoc
      .MainSettings()
      .withOut(Paths.get("website", "target", "docs"))
      .withSiteVariables(
        Map(
          "GITTER" -> "[Gitter](http://gitter.im/scalacenter/scalafix)",
          "SCALA212" -> Versions.scala212,
          "SCALA211" -> Versions.scala211,
          "NIGHTLY_VERSION" -> Versions.version,
          "VERSION" -> Versions.stableVersion,
          "SCALAMETA" -> Versions.scalameta
        )
      )
      .withStringModifiers(
        List(
          new HelpModifier,
          new RulesModifier
        )
      )
      .withArgs(args.toList)
    val exit = mdoc.Main.process(settings)
    sys.exit(exit)
  }
}
