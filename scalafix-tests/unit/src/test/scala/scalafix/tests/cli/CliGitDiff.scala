package scalafix.tests.cli

import org.scalatest.FunSuite

import scalafix.cli.Cli
import scalafix.internal.cli.CommonOptions
import java.io.ByteArrayOutputStream
import java.io.PrintStream

import java.nio.charset.StandardCharsets

import collection.immutable.Seq

class CliGitDiff extends FunSuite {
  test("foo") {
    val baos = new ByteArrayOutputStream()
    val ps = new PrintStream(baos)

    val exit = Cli.runMain(
      Seq(
        "--rules",
        "DisableSyntax",
        "--diff"
      ),
      CommonOptions(
        workingDirectory = "..",
        out = ps,
        err = ps
      )
    )

    val content = new String(baos.toByteArray(), StandardCharsets.UTF_8);

    println(content)
  }
}
