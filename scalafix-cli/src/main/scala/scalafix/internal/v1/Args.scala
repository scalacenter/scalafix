package scalafix.internal.v1

import java.io.File
import java.io.PrintStream
import java.net.URI
import java.net.URLClassLoader
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.nio.file.PathMatcher
import java.nio.file.Paths
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

import scala.annotation.StaticAnnotation
import scala.jdk.CollectionConverters._
import scala.util.Failure
import scala.util.Success
import scala.util.Try

import scala.meta.internal.io.PathIO
import scala.meta.internal.symtab.SymbolTable
import scala.meta.io.AbsolutePath
import scala.meta.io.Classpath

import coursierapi.MavenRepository
import coursierapi.Repository
import metaconfig.Configured._
import metaconfig._
import metaconfig.annotation._
import metaconfig.generic.Surface
import metaconfig.internal.ConfGet
import metaconfig.typesafeconfig.typesafeConfigMetaconfigParser
import scalafix.interfaces.ScalafixMainCallback
import scalafix.internal.config.FilterMatcher
import scalafix.internal.config.PrintStreamReporter
import scalafix.internal.config.ScalaVersion
import scalafix.internal.config.ScalaVersion.scala2
import scalafix.internal.config.ScalafixConfig
import scalafix.internal.diff.DiffDisable
import scalafix.internal.interfaces.MainCallbackImpl
import scalafix.internal.jgit.JGitDiff
import scalafix.internal.reflect.ClasspathOps
import scalafix.v1.Configuration
import scalafix.v1.RuleDecoder

class Section(val name: String) extends StaticAnnotation

case class Args(
    @Section("Common options")
    @Description(
      "Scalafix rules to run, for example ExplicitResultTypes. " +
        "The syntax for rules is documented in https://scalacenter.github.io/scalafix/docs/users/configuration#rules"
    )
    @ExtraName("r")
    @Repeated
    rules: List[String] = Nil,
    @Description("Files or directories (recursively visited) to fix.")
    @ExtraName("remainingArgs")
    @ExtraName("f")
    files: List[AbsolutePath] = Nil,
    @Description(
      "File path to a .scalafix.conf configuration file. " +
        "Defaults to .scalafix.conf in the current working directory, if any."
    )
    config: Option[AbsolutePath] = None,
    @Description(
      "Check that all files have been fixed with scalafix, exiting with non-zero code on violations. " +
        "Won't write to files."
    )
    @ExtraName("test")
    check: Boolean = false,
    @Description("Print fixed output to stdout instead of writing in-place.")
    stdout: Boolean = false,
    @Description(
      "If set, only apply scalafix to added and edited files in git diff against the master branch."
    )
    diff: Boolean = false,
    @Description(
      "If set, only apply scalafix to added and edited files in git diff against a provided branch, commit or tag."
    )
    diffBase: Option[String] = None,
    @Description(
      "The major or binary Scala version that the provided files are targeting, " +
        "or the full version that was used to compile them when a classpath is provided."
    )
    scalaVersion: ScalaVersion = Args.runtimeScalaVersion,
    @Description(
      "Run only syntactic rules, ignore semantic rules even if they are explicitly " +
        "configured in .scalafix.conf or via --rules"
    )
    syntactic: Boolean = false,
    @Description(
      "Overlay the default rules & rule settings in .scalafix.conf with the `triggered` section"
    )
    triggered: Boolean = false,
    @Description("Print out additional diagnostics while running scalafix.")
    verbose: Boolean = false,
    @Description("Print out this help message and exit")
    @ExtraName("h")
    help: Boolean = false,
    @Description("Print out version number and exit")
    @ExtraName("v")
    version: Boolean = false,
    @Section("Semantic options")
    @Description(
      "Full classpath of the files to fix, required for semantic rules. " +
        "The source files that should be fixed must be compiled with semanticdb-scalac. " +
        "Dependencies are required by rules like ExplicitResultTypes, but the dependencies do not " +
        "need to be compiled with semanticdb-scalac."
    )
    classpath: Classpath = Classpath(Nil),
    @Description(
      "Absolute path passed to semanticdb with -P:semanticdb:sourceroot:<path>. " +
        "Relative filenames persisted in the Semantic DB are absolutized by the " +
        "sourceroot. Defaults to current working directory if not provided."
    )
    sourceroot: Option[AbsolutePath] = None,
    @Description(
      "Absolute paths passed to semanticdb with -P:semanticdb:targetroot:<path>. " +
        "Used to locate semanticdb files. By default, Scalafix will try to locate semanticdb files in the classpath"
    )
    semanticdbTargetroots: List[AbsolutePath] = Nil,
    @Description(
      "If set, automatically infer the --classpath flag by scanning for directories with META-INF/semanticdb"
    )
    autoClasspath: Boolean = false,
    @Description("Additional directories to scan for --auto-classpath")
    @ExtraName("classpathAutoRoots")
    autoClasspathRoots: List[AbsolutePath] = Nil,
    @Description(
      "The scala compiler options used to compile this --classpath, for example -Ywarn-unused-import"
    )
    scalacOptions: List[String] = Nil,
    @Section("Tab completions")
    @Description(
      """|Print out bash tab completions. To install:
        |```
        |# macOS, requires "brew install bash-completion"
        |scalafix --bash > /usr/local/etc/bash_completion.d/scalafix
        |# Linux
        |scalafix --bash > /etc/bash_completion.d/scalafix
        |```
        |""".stripMargin
    )
    bash: Boolean = false,
    @Description(
      """|Print out zsh tab completions. To install:
        |```
        |scalafix --zsh > /usr/local/share/zsh/site-functions/_scalafix
        |unfunction _scalafix
        |autoload -U _scalafix
        |```
        |""".stripMargin
    )
    zsh: Boolean = false,
    // TODO: --sbt
    @Section("Less common options")
    @Description(
      "Unix-style glob for files to exclude from fixing. " +
        "The glob syntax is defined by `nio.FileSystem.getPathMatcher`."
    )
    exclude: List[PathMatcher] = Nil,
    @Description("Maven repositories to fetch the artifacts from")
    repositories: List[Repository] = Repository.defaults.asScala.toList,
    @Description(
      "Additional classpath for compiling and classloading custom rules, as a set of filesystem paths, separated by ':' on Unix or ';' on Windows."
    )
    toolClasspath: URLClassLoader = ClasspathOps.thisClassLoader,
    @Description("The encoding to use for reading/writing files")
    charset: Charset = StandardCharsets.UTF_8,
    @Description("If set, throw exception in the end instead of System.exit")
    noSysExit: Boolean = false,
    @Description("Don't error on stale semanticdb files.")
    noStaleSemanticdb: Boolean = false,
    @Description("Custom settings to override .scalafix.conf")
    settings: Conf = Conf.Obj.empty,
    @Description(
      "Write fixed output to custom location instead of in-place. Regex is passed as first argument to file.replaceAll(--out-from, --out-to), requires --out-to."
    )
    outFrom: Option[String] = None,
    @Description(
      "Companion of --out-from, string that is passed as second argument to fileToFix.replaceAll(--out-from, --out-to)"
    )
    outTo: Option[String] = None,
    @Description(
      "Insert /* scalafix:ok */ suppressions instead of reporting linter errors."
    )
    autoSuppressLinterErrors: Boolean = false,
    @Description("The current working directory")
    cwd: AbsolutePath,
    @Hidden
    @Description("No longer used")
    nonInteractive: Boolean = false,
    @Hidden
    out: PrintStream,
    @Hidden
    ls: Ls = Ls.Find,
    @Hidden
    callback: ScalafixMainCallback
) {

  override def toString: String = ConfEncoder[Args].write(this).toString()

  def configuredSymtab: Configured[SymbolTable] = {
    Try(
      ClasspathOps.newSymbolTable(
        classpath = validatedClasspath,
        out = out
      )
    ) match {
      case Success(symtab) =>
        Configured.ok(symtab)
      case Failure(e) =>
        ConfError.message(s"Unable to load symbol table: ${e.getMessage}").notOk
    }
  }

  def baseConfig: Configured[(Conf, ScalafixConfig, DelegatingMainCallback)] = {
    fileConfig.andThen { b =>
      val applied = Conf.applyPatch(b, settings)
      applied.as[ScalafixConfig].map { scalafixConfig =>
        val delegator = new DelegatingMainCallback(callback)
        val reporter = MainCallbackImpl.fromJava(delegator)
        (applied, scalafixConfig.copy(reporter = reporter), delegator)
      }
    }
  }

  def fileConfig: Configured[Conf] = {
    val toRead: Option[AbsolutePath] = config.orElse {
      val defaultPath = cwd.resolve(".scalafix.conf")
      if (defaultPath.isFile) Some(defaultPath)
      else None
    }
    toRead match {
      case Some(file) =>
        if (file.isFile) {
          val input = metaconfig.Input.File(file.toNIO)
          Conf.parseInput(input)
        } else {
          ConfError.fileDoesNotExist(file.toNIO).notOk
        }
      case _ =>
        Configured.ok(Conf.Obj.empty)
    }
  }

  def ruleDecoderSettings: RuleDecoder.Settings = {
    RuleDecoder
      .Settings()
      .withToolClasspath(toolClasspath)
      .withCwd(cwd)
      .withSyntactic(syntactic)
  }

  def ruleDecoder(scalafixConfig: ScalafixConfig): ConfDecoder[Rules] = {
    RuleDecoder.decoder(ruleDecoderSettings.withConfig(scalafixConfig))
  }

  // With a --triggered flag, looking for settings in triggered block first, and fallback to standard settings.
  def maybeOverlaidConfWithTriggered(base: Conf): Conf =
    if (triggered) {
      val triggeredOverlay = ConfGet
        .getOrOK(base, "triggered" :: Nil, Configured.ok, Conf.Obj.empty)
        .getOrElse(Conf.Obj.empty)
      ConfOps.merge(base, triggeredOverlay)
    } else base

  def rulesConf(base: () => Conf): Conf = {
    if (rules.isEmpty) {
      ConfGet
        .getOrOK(
          maybeOverlaidConfWithTriggered(base()),
          "rules" :: "rule" :: Nil,
          Configured.ok,
          Conf.Lst(Nil)
        )
        .getOrElse(Conf.Lst(Nil))
    } else {
      Conf.Lst(rules.map(Conf.fromString))
    }
  }

  def configuredRules(
      base: Conf,
      scalafixConfig: ScalafixConfig
  ): Configured[Rules] = {
    val targetConf = maybeOverlaidConfWithTriggered(base)

    val rulesConf = this.rulesConf(() => base)
    val decoder = ruleDecoder(scalafixConfig)

    val configuration = Configuration()
      .withConf(targetConf)
      .withScalaVersion(scalaVersion.value)
      .withScalacOptions(scalacOptions)
      .withScalacClasspath(validatedClasspath.entries)
    decoder.read(rulesConf).andThen(_.withConfiguration(configuration))
  }

  def resolvedPathReplace: Configured[AbsolutePath => AbsolutePath] =
    (outFrom, outTo) match {
      case (None, None) => Ok(identity[AbsolutePath])
      case (Some(from), Some(to)) =>
        try {
          val outFromPattern = Pattern.compile(from)
          def replacePath(file: AbsolutePath): AbsolutePath = AbsolutePath(
            Paths.get(
              URI.create(
                "file:" +
                  outFromPattern.matcher(file.toURI.getPath).replaceAll(to)
              )
            )
          )
          Ok(replacePath _)
        } catch {
          case e: PatternSyntaxException =>
            ConfError
              .message(s"Invalid regex '$outFrom'! ${e.getMessage}")
              .notOk
        }
      case (Some(from), _) =>
        ConfError
          .message(s"--out-from $from must be accompanied with --out-to")
          .notOk
      case (_, Some(to)) =>
        ConfError
          .message(s"--out-to $to must be accompanied with --out-from")
          .notOk
    }

  def configuredDiffDisable: Configured[DiffDisable] = {
    if (diff || diffBase.nonEmpty) {
      val diffBase = this.diffBase.getOrElse("master")
      JGitDiff(cwd.toNIO, diffBase)
    } else {
      Configured.Ok(DiffDisable.empty)
    }
  }

  def sourceScalaVersion: Option[ScalaVersion] =
    extractLastScalacOption("-Xsource:")
      .map(_.stripSuffix("-cross"))
      .flatMap(ScalaVersion.from(_).toOption)

  def configureScalaVersions(
      conf: ScalafixConfig
  ): Configured[ScalafixConfig] = {
    Configured.ok(
      conf.copy(
        scalaVersion = scalaVersion,
        sourceScalaVersion = sourceScalaVersion
      )
    )
  }

  def configuredSourceroot: Configured[AbsolutePath] = {
    val path = sourceroot.getOrElse(cwd)
    if (path.isDirectory) Configured.ok(path)
    else Configured.error(s"--sourceroot $path is not a directory")
  }

  def validatedClasspath: Classpath = {
    val targetrootClasspath = semanticdbTargetroots match {
      case Nil =>
        semanticdbOption("targetroot", Some("-semanticdb-target")).toList
          .map(AbsolutePath.apply)
      case _ => semanticdbTargetroots
    }
    val baseClasspath =
      if (autoClasspath && classpath.entries.isEmpty) {
        val roots =
          if (autoClasspathRoots.isEmpty) cwd :: Nil
          else autoClasspathRoots
        ClasspathOps.autoClasspath(roots)
      } else {
        classpath
      }
    Classpath(targetrootClasspath) ++ baseClasspath
  }

  def classLoader: ClassLoader =
    ClasspathOps.toOrphanClassLoader(validatedClasspath)

  private def extractLastScalacOption(flag: String) = {
    scalacOptions
      .filter(_.startsWith(flag))
      .lastOption
      .map(_.stripPrefix(flag))
  }

  private def semanticdbOption(
      settingInScala2: String,
      settingInScala3Opt: Option[String]
  ): Option[String] = {
    if (scalaVersion.isScala2) {
      extractLastScalacOption(s"-P:semanticdb:$settingInScala2:")
    } else {
      settingInScala3Opt.flatMap { settingInScala3 =>
        scalacOptions
          .sliding(2)
          .collectFirst {
            case h :: last :: Nil if h == settingInScala3 => last
          }
      }
    }
  }

  def semanticdbFilterMatcher: FilterMatcher = {
    val include = semanticdbOption("include", None)
    val exclude = semanticdbOption("exclude", None)
    (include, exclude) match {
      case (None, None) => FilterMatcher.matchEverything
      case (Some(in), None) => FilterMatcher.include(in)
      case (None, Some(ex)) => FilterMatcher.exclude(ex)
      case (Some(in), Some(ex)) => FilterMatcher(List(in), List(ex))
    }
  }

  def validate: Configured[ValidatedArgs] = {
    baseConfig.andThen { case (base, scalafixConfig, delegator) =>
      (
        configuredSourceroot |@|
          configuredSymtab |@|
          configuredRules(base, scalafixConfig) |@|
          resolvedPathReplace |@|
          configuredDiffDisable |@|
          configureScalaVersions(scalafixConfig)
      ).map {
        case (
              ((((root, symtab), rulez), pathReplace), diffDisable),
              scalafixConfig
            ) =>
          ValidatedArgs(
            this,
            symtab,
            rulez,
            scalafixConfig,
            classLoader,
            root,
            pathReplace,
            diffDisable,
            delegator,
            semanticdbFilterMatcher
          )
      }
    }
  }
}

object Args extends TPrintImplicits {
  val baseMatcher: PathMatcher =
    FileSystems.getDefault.getPathMatcher("glob:**.{scala,sbt,sc}")
  val runtimeScalaVersion: ScalaVersion = ScalaVersion
    .from(scala.util.Properties.versionNumberString) // can be empty
    .toOption
    .getOrElse(scala2)

  val default: Args = default(PathIO.workingDirectory, System.out)
  def default(cwd: AbsolutePath, out: PrintStream): Args = {
    val callback = MainCallbackImpl.fromScala(PrintStreamReporter(out))
    new Args(cwd = cwd, out = out, callback = callback)
  }

  def decoder(base: Args): ConfDecoder[Args] = {
    implicit val classpathDecoder: ConfDecoder[Classpath] =
      ConfDecoder.stringConfDecoder.map { cp =>
        Classpath(
          cp.split(File.pathSeparator)
            .iterator
            .map(path => AbsolutePath(path)(base.cwd))
            .toList
        )
      }
    implicit val classLoaderDecoder: ConfDecoder[URLClassLoader] =
      ConfDecoder[Classpath].map(ClasspathOps.toClassLoader)
    implicit val absolutePathDecoder: ConfDecoder[AbsolutePath] =
      ConfDecoder.stringConfDecoder.map(AbsolutePath(_)(base.cwd))
    generic.deriveDecoder(base)
  }

  implicit val charsetDecoder: ConfDecoder[Charset] =
    ConfDecoder.stringConfDecoder.map(name => Charset.forName(name))
  implicit val printStreamDecoder: ConfDecoder[PrintStream] =
    ConfDecoder.stringConfDecoder.map(_ => System.out)
  implicit val pathMatcherDecoder: ConfDecoder[PathMatcher] =
    ConfDecoder.stringConfDecoder.map(glob =>
      FileSystems.getDefault.getPathMatcher("glob:" + glob)
    )
  implicit val repositoryDecoder: ConfDecoder[Repository] =
    ConfDecoder.stringConfDecoder.map(base => MavenRepository.of(base))
  implicit val scalaVersionDecoder: ConfDecoder[ScalaVersion] =
    ScalafixConfig.scalaVersionDecoder

  implicit val scalaVersionEncoder: ConfEncoder[ScalaVersion] =
    ConfEncoder.StringEncoder.contramap(_.value)
  implicit val callbackDecoder: ConfDecoder[ScalafixMainCallback] =
    ConfDecoder.stringConfDecoder.map(_ => MainCallbackImpl.default)

  implicit val confEncoder: ConfEncoder[Conf] =
    ConfEncoder.ConfEncoder
  implicit val pathEncoder: ConfEncoder[AbsolutePath] =
    ConfEncoder.StringEncoder.contramap(_.toString())
  implicit val classpathEncoder: ConfEncoder[Classpath] =
    ConfEncoder.StringEncoder.contramap(_ => "<classpath>")
  implicit val classLoaderEncoder: ConfEncoder[URLClassLoader] =
    ConfEncoder.StringEncoder.contramap(_ => "<classloader>")
  implicit val charsetEncoder: ConfEncoder[Charset] =
    ConfEncoder.StringEncoder.contramap(_.name())
  implicit val printStreamEncoder: ConfEncoder[PrintStream] =
    ConfEncoder.StringEncoder.contramap(_ => "<stdout>")
  implicit val pathMatcherEncoder: ConfEncoder[PathMatcher] =
    ConfEncoder.StringEncoder.contramap(_.toString)
  implicit val repositoriesEncoder: ConfEncoder[Repository] =
    ConfEncoder.StringEncoder.contramap(_.toString)
  implicit val callbackEncoder: ConfEncoder[ScalafixMainCallback] =
    ConfEncoder.StringEncoder.contramap(_.toString)
  implicit val argsEncoder: ConfEncoder[Args] = generic.deriveEncoder

  implicit val argsSurface: Surface[Args] = generic.deriveSurface
}
