package scalafix.internal.cli

import java.io.File
import java.io.InputStream
import java.io.PrintStream
import scala.meta._
import scala.meta.io.AbsolutePath
import scalafix.config.PrintStreamReporter
import scalafix.config.ScalafixReporter
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
  def reporter: PrintStreamReporter =
    ScalafixReporter.default.copy(outStream = err)
  def workingPath = AbsolutePath(workingDirectory)
  def workingDirectoryFile = new File(workingDirectory)
}

// NOTE: Do not depend on this class as a library, the interface will change between
// patch versions.
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
      "Absolute path passed to scalahost with -P:scalahost:sourceroot:<path>. " +
        "Relative filenames persisted in the Semantic DB are absolutized by the " +
        "sourceroot. Defaults to current working directory if not provided.")
    @ValueDescription("/foo/myproject")
    sourceroot: Option[String] = None,
    @HelpMessage(
      "java.io.File.pathSeparator separated list of directories or jars containing " +
        "'.semanticdb' files. The 'semanticdb' files are emitted by the " +
        "scalahost-nsc compiler plugin and are necessary for semantic rewrites " +
        "like ExplicitReturnTypes to function."
    )
    @ValueDescription("entry1.jar:entry2.jar:target/scala-2.12/classes")
    classpath: Option[String] = None,
    @HelpMessage(
      s"""Rewrite rules to run.""".stripMargin
    )
    @ValueDescription(
      s"$ProcedureSyntax OR " +
        s"file:LocalFile.scala OR " +
        s"scala:full.Name OR " +
        s"https://gist.com/.../Rewrite.scala"
    )
    @HelpMessage(
      s"Space separated list of rewrites to run. Available options include: " +
        s"${ScalafixRewrites.allNames.mkString(", ")}")
    @ExtraName("r")
    rewrites: List[String] = Nil,
    @ExtraName("f")
    @Hidden // hidden because it's optional. The help message explains how files are chosen.
    files: List[String] = List.empty[String],
    @HelpMessage("If set, print fix to stdout instead of writing to file.")
    stdout: Boolean = false,
    @HelpMessage(
      "Regex that is passed as first argument to fileToFix.replaceAll(outFrom, outTo)"
    )
    @ValueDescription("/shared/")
    outFrom: Option[String] = None,
    @HelpMessage(
      "Replacement string that is passed as second argument to fileToFix.replaceAll(outFrom, outTo)")
    @ValueDescription("/custom/")
    outTo: Option[String] = None,
    @HelpMessage(
      "Space separated list of regexes to exclude which files to fix. If a file " +
        "match one of the exclude regexes, then it will not get fixed. " +
        "Defaults to excluding no files.")
    @ValueDescription("core Foobar.scala")
    exclude: List[String] = Nil,
    @HelpMessage(
      "If true, run on single thread. If false (default), use all available cores")
    singleThread: Boolean = false,
    // NOTE: This option could be avoided by adding another entrypoint like 'Cli.safeMain'
    // or SafeCli.main. However, I opted for a cli flag since that plays nicely
    // with default 'run.in(Compile).toTask("--no-sys-exit").value' in sbt.
    // Another other option would be to do
    // 'runMain.in(Compile).toTask("scalafix.cli.SafeMain")' but I prefer to
    // keep only one main function if possible since that plays nicely with
    // automatic detection of 'main' functions in tools like 'coursier launch'.
    @HelpMessage(
      "If true, does not sys.exit at the end. Useful for example in sbt-scalafix")
    noSysExit: Boolean = false,
    // Here for backwards compat with scripts.
    @Hidden
    @ExtraName("i")
    inPlace: Boolean = true,
    @Recurse common: CommonOptions = CommonOptions(),
    @HelpMessage(
      """Don't report parse errors for non-explictly passed filepaths.""".stripMargin)
    quietParseErrors: Boolean = false,
    @HelpMessage(
      """Print out bash completion file for scalafix. To install on
        |          scalafix --bash > /usr/local/etc/bash_completion.d/scalafix # Mac
        |          scalafix --bash > /etc/bash_completion.d/scalafix           # Linux""".stripMargin)
    bash: Boolean = false,
    @HelpMessage(
      """Print out zsh completion file for scalafix. To install:
        |          scalafix --zsh > /usr/local/share/zsh/site-functions/_scalafix""".stripMargin)
    zsh: Boolean = false
)
