package scalafix
package cli

import java.io.File
import java.io.InputStream
import java.io.PrintStream
import scala.meta._
import scala.meta.io.AbsolutePath
import scalafix.rewrite.ProcedureSyntax
import scalafix.rewrite.ScalafixRewrites
import caseapp._
import metaconfig.Conf
import metaconfig.ConfError
import metaconfig.Configured
import metaconfig.Configured.NotOk
import metaconfig.Configured.Ok

case class CommonOptions(
    @Hidden workingDirectory: String = System.getProperty("user.dir"),
    @Hidden out: PrintStream = System.out,
    @Hidden in: InputStream = System.in,
    @Hidden err: PrintStream = System.err,
    @Hidden stackVerbosity: Int = 20
) {
  def workingPath = AbsolutePath(workingDirectory)
  def workingDirectoryFile = new File(workingDirectory)
}

@AppName("scalafix")
@AppVersion(scalafix.Versions.version)
@ProgName("scalafix")
case class ScalafixOptions(
    @HelpMessage("Print version number and exit")
    @ExtraName("v")
    version: Boolean = false,
    @HelpMessage("If set, print out debugging inforation to stderr.")
    verbose: Boolean = false,
    @HelpMessage("File path to a .scalafix.conf configuration file.")
    @ValueDescription(".scalafix.conf")
    @ExtraName("c")
    config: Option[String] = None,
    @HelpMessage("String representing scalafix configuration")
    @ValueDescription("imports.organize=false")
    @ExtraName("c")
    configStr: Option[String] = None,
    @HelpMessage(
      """Absolute path passed to scalahost with -P:scalahost:sourceroot:<path>.
        |        Relative filenames persisted in the Semantic DB are absolutized
        |        by the sourceroot. Required for semantic rewrites.
      """.stripMargin)
    @ValueDescription("/foo/myproject")
    sourceroot: Option[String] = None,
    @HelpMessage(
      """java.io.File.pathSeparator separated list of jar files or directories
        |        containing classfiles and `semanticdb` files. The `semanticdb`
        |        files are emitted by the scalahost-nsc compiler plugin and
        |        are necessary for the semantic API to function. The
        |        classfiles + jar files are necessary forruntime compilation
        |        of quasiquotes when extracting symbols (that is,
        |        `q"scala.Predef".symbol`)""".stripMargin
    )
    @ValueDescription("entry1.jar:entry2.jar")
    classpath: Option[String] = None,
    @Hidden // TODO(olafur) remove in v0.4. This is not used.
    @HelpMessage(
      """java.io.File.pathSeparator separated list of Scala source files OR
        |        directories containing Scala source files""".stripMargin)
    @ValueDescription("File2.scala:File1.scala:src/main/scala")
    sourcepath: Option[String] = None,
    @HelpMessage(
      s"""Rewrite rules to run.
         |        NOTE. rewrite.rules = [ .. ] from --config will also run""".stripMargin
    )
    @ValueDescription(
      s"""$ProcedureSyntax OR
         |               file:LocalFile.scala OR
         |               scala:full.Name OR
         |               https://gist.com/.../Rewrite.scala""".stripMargin
    )
    @HelpMessage(
      s"Space separated list of rewrites to run. Available options include: " +
        s"${ScalafixRewrites.allNames.mkString(", ")}")
    rewrites: List[String] = Nil,
    @HelpMessage(
      "Files to fix. Runs on all *.scala files if given a directory")
    @ValueDescription("File1.scala File2.scala")
    @ExtraName("f")
    files: List[String] = List.empty[String],
    @HelpMessage(
      "If true, writes changes to files instead of printing to stdout")
    @ExtraName("i")
    inPlace: Boolean = true,
    stdout: Boolean = false,
    @HelpMessage(
      """Regex that is passed as first argument to
        |        fileToFix.replaceAll(outFrom, outTo)""".stripMargin
    )
    @ValueDescription("/shared/")
    outFrom: String = "",
    @HelpMessage(
      """Replacement string that is passed as second argument to
        |        fileToFix.replaceAll(outFrom, outTo)""".stripMargin
    )
    @ValueDescription("/custom/")
    outTo: String = "",
    @HelpMessage(
      "If true, run on single thread. If false (default), use all available cores")
    singleThread: Boolean = false,
    // NOTE: This option could be avoided by adding another entrypoint like `Cli.safeMain`
    // or SafeCli.main. However, I opted for a cli flag since that plays nicely
    // with default `run.in(Compile).toTask("--no-sys-exit").value` in sbt.
    // Another other option would be to do
    // `runMain.in(Compile).toTask("scalafix.cli.SafeMain")` but I prefer to
    // keep only one main function if possible since that plays nicely with
    // automatic detection of `main` functions in tools like `coursier launch`.
    @HelpMessage(
      "If true, does not sys.exit at the end. Useful for example in sbt-scalafix")
    noSysExit: Boolean = false,
    @Recurse common: CommonOptions = CommonOptions()
)
