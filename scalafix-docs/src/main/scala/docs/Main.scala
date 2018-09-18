package docs

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}

import scalafix.Versions
import scalafix.docs.PatchDocs

import scala.meta.internal.io.{FileIO, PathIO}

object Main {
  def copyContributingGuide(out: Path): Unit = {
    val guide = FileIO
      .slurp(
        PathIO.workingDirectory.resolve("CONTRIBUTING.md"),
        StandardCharsets.UTF_8)
      .stripPrefix("# Contributing\n")
    val text =
      """---
id: contributing
title: Contributing Guide
sidebar_label: Guide
---
""".stripMargin + guide
    val outfile = out.resolve("developers").resolve("contributing.md")
    Files.createDirectories(outfile.getParent)
    Files.write(outfile, text.getBytes(StandardCharsets.UTF_8))
  }
  def main(args: Array[String]): Unit = {
    val out = Paths.get("website", "target", "docs")
    copyContributingGuide(out)
    val settings = mdoc
      .MainSettings()
      .withOut(out)
      .withSiteVariables(
        Map(
          "SEMANTICDB" -> "[SemanticDB](https://scalameta.org/docs/semanticdb/specification.html)",
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
