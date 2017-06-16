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
      """java.io.File.pathSeparator separated list of directories containing
        |        '.semanticdb' files. The 'semanticdb' files are emitted by the
        |        scalahost-nsc compiler plugin and are necessary for semantic
        |        rewrites like ExplicitReturnTypes to function.""".stripMargin
    )
    @ValueDescription("entry1.jar:entry2.jar")
    classpath: Option[String] = None,
    @HelpMessage("""automatically look for semanticdb files (EXPERIMENTAL).""")
    autoMirror: Boolean = false,
    @HelpMessage(
      s"""Rewrite rules to run.""".stripMargin
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
    @ExtraName("rewrite")
    rewrites: List[String] = Nil,
    @HelpMessage(
      """.scala files to fix, recurses on directories.""".stripMargin)
    @ValueDescription("File1.scala File2.scala")
    @ExtraName("f")
    files: List[String] = List.empty[String],
    @HelpMessage("If set, print fix to stdout instead of writing to file.")
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
      """Space separated list of regexes to filter files from the Semantic DB
        |        to be included for fixing. This is the parallel of --files
        |        but for semantic reerites. A file must match one of the regexes
        |        to be included. Defaults to matching all files.
      """.stripMargin)
    @ValueDescription("core Foobar.scala")
    include: List[String] = List(".*"),
    @HelpMessage(
      """Space separated list of regexes to exclude which files to fix.
        |        If a file match one of the exclude regexes, then it will not
        |        get fixed. Defaults to excluding no files.
      """.stripMargin)
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
        |        Mac:
        |        scalafix --bash > /usr/local/etc/bash_completion.d/scalafix
        |        Linux:
        |        scalafix --bash > /etc/bash_completion.d/scalafix""".stripMargin)
    bash: Boolean = false,
    @HelpMessage(
      """Print out zsh completion file for scalafix. To install:
        |
        |        scalafix --zsh > /usr/local/share/zsh/site-functions/_scalafix""".stripMargin)
    zsh: Boolean = false
)
