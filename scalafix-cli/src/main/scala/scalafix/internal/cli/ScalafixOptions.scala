package scalafix.internal.cli

import java.io.File
import java.io.InputStream
import java.io.PrintStream
import scala.meta._
import scala.meta.io.AbsolutePath
import scalafix.internal.config.OutputFormat
import scalafix.internal.config.PrintStreamReporter
import scalafix.internal.config.ScalafixReporter
import scalafix.internal.rule.ProcedureSyntax
import scalafix.rule.ScalafixRules
import caseapp._

case class CommonOptions(
    @Hidden workingDirectory: String = System.getProperty("user.dir"),
    @Hidden out: PrintStream = System.out,
    @Hidden in: InputStream = System.in,
    @Hidden err: PrintStream = System.err,
    @Hidden stackVerbosity: Int = 20
) {
  lazy val cliArg: PrintStreamReporter =
    ScalafixReporter.default.copy(outStream = out)
  def workingPath = AbsolutePath(workingDirectory)
  def workingDirectoryFile = new File(workingDirectory)
}

object CommonOptions {
  lazy val default = CommonOptions()
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
    @HelpMessage("Absolute path passed to semanticdb with -P:semanticdb:sourceroot:<path>. " +
      "Relative filenames persisted in the Semantic DB are absolutized by the " +
      "sourceroot. Defaults to current working directory if not provided.")
    @ValueDescription("/foo/myproject")
    sourceroot: Option[String] = None,
    @HelpMessage(
      "java.io.File.pathSeparator separated list of directories or jars containing " +
        "'.semanticdb' files. The 'semanticdb' files are emitted by the " +
        "semanticdb-scalac compiler plugin and are necessary for semantic rules " +
        "like ExplicitResultTypes to function."
    )
    @ValueDescription("entry1.jar:entry2.jar:target/scala-2.12/classes/")
    classpath: Option[String] = None,
    @HelpMessage(
      "Automatically infer --classpath starting from these directories. " +
        "Ignored if --classpath is provided.")
    classpathAutoRoots: Option[String] = None,
    @HelpMessage(
      "Additional classpath to use when classloading/compiling rules")
    @ValueDescription("entry1.jar:entry2.jar:target/scala-2.12/classes/")
    toolClasspath: Option[String] = None,
    @HelpMessage("Disable validation when loading semanticdb files.")
    noStrictSemanticdb: Boolean = false,
    @HelpMessage(
      s"""Scalafix rules to run.""".stripMargin
    )
    @ValueDescription(
      s"$ProcedureSyntax OR " +
        s"file:LocalFile.scala OR " +
        s"scala:full.Name OR " +
        s"https://gist.com/.../Rule.scala"
    )
    @HelpMessage(
      s"Space separated list of rules to run. Available options include: " +
        s"${ScalafixRules.allNames.mkString(", ")}")
    @ExtraName("r")
    rules: List[String] = Nil,
    @ExtraName("f")
    @Hidden // hidden because it's optional. The help message explains how files are chosen.
    files: List[String] = List.empty[String],
    @HelpMessage("If set, print fix to stdout instead of writing to file.")
    stdout: Boolean = false,
    @HelpMessage(
      "Exit non-zero code if files have not been fixed. Won't write to files.")
    test: Boolean = false,
    @HelpMessage("Write to files. In case of linter error adds a comment to suppress the error.")
    suppress: Boolean = false,
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
    zsh: Boolean = false,
    @HelpMessage("Don't use fancy progress bar.")
    nonInteractive: Boolean = false,
    @HelpMessage("String ID to prefix reported messages with")
    projectId: Option[String] = None,
    @HelpMessage("""Print out sbt completion parser to argument.""".stripMargin)
    @ValueDescription("scalafix-sbt/src/main/scala/Completions.scala")
    @Hidden
    sbt: Option[String] = None,
    @HelpMessage(
      "If set, only apply scalafix to added and edited files in git diff against master.")
    diff: Boolean = false,
    @HelpMessage(
      "If set, only apply scalafix to added and edited files in git diff against a provided branch, commit or tag. (defaults to master)")
    diffBase: Option[String] = None,
    format: Option[OutputFormat] = None
) {
  def classpathRoots: List[AbsolutePath] =
    classpathAutoRoots.fold(List(common.workingPath))(cp =>
      Classpath(cp).shallow)
  def projectIdPrefix: String = projectId.fold("")(id => s"[$id] ")
  lazy val diagnostic: ScalafixReporter =
    ScalafixReporter.default.copy(
      outStream = if (stdout) common.err else common.out
    )
}
