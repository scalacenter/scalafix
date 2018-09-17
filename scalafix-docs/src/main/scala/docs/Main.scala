package docs

import java.nio.file.Paths

import scalafix.Versions
import scalafix.docs.PatchDocs

object Main {
  def main(args: Array[String]): Unit = {
    val settings = mdoc
      .MainSettings()
      .withOut(Paths.get("website", "target", "docs"))
      .withSiteVariables(
        Map(
          "SEMANTICDB" -> "[SemanticDB](https://github.com/scalameta/scalameta/blob/master/semanticdb/semanticdb3/semanticdb3.md)",
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
          new FileModifier,
          new HelpModifier,
          new RuleModifier,
          new RulesModifier
        )
      )
      .withArgs(args.toList)
    val exit = mdoc.Main.process(settings)
    PatchDocs.compiler.askShutdown()
    if (exit != 0) sys.exit(exit)
  }
}
