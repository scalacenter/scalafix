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
import metaconfig.Configured._
import metaconfig._
import metaconfig.annotation._
import metaconfig.generic.Surface
import metaconfig.internal.ConfGet
import metaconfig.typesafeconfig.typesafeConfigMetaconfigParser
import pprint.TPrint
import scala.annotation.StaticAnnotation
import scala.meta.internal.io.PathIO
import scala.meta.internal.symtab.SymbolTable
import scala.meta.io.AbsolutePath
import scala.meta.io.Classpath
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scalafix.interfaces.ScalafixMainCallback
import scalafix.internal.config.FilterMatcher
import scalafix.internal.config.PrintStreamReporter
import scalafix.internal.config.ScalafixConfig
import scalafix.internal.diff.DiffDisable
import scalafix.internal.interfaces.MainCallbackImpl
import scalafix.internal.jgit.JGitDiff
import scalafix.internal.reflect.ClasspathOps
import scalafix.v1.Configuration
import scalafix.v1.RuleDecoder
import scala.tools.nsc.interactive.Global
import scala.tools.nsc.Settings
import scala.tools.nsc.reporters.ConsoleReporter

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
      "Run only syntactic rules, ignore semantic rules even if they are explicitly " +
        "configured in .scalafix.conf or via --rules"
    )
    syntactic: Boolean = false,
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
    @Description(
      "The Scala compiler version that was used to compile this project."
    )
    scalaVersion: String = scala.util.Properties.versionNumberString,
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

  def rulesConf(base: () => Conf): Conf = {
    if (rules.isEmpty) {
      ConfGet.getKey(base(), "rules" :: "rule" :: Nil) match {
        case Some(c) => c
        case _ => Conf.Lst(Nil)
      }
    } else {
      Conf.Lst(rules.map(Conf.fromString))
    }
  }

  def configuredRules(
      base: Conf,
      scalafixConfig: ScalafixConfig
  ): Configured[Rules] = {
    val rulesConf = this.rulesConf(() => base)
    val decoder = ruleDecoder(scalafixConfig)
    val configuration = Configuration()
      .withConf(base)
      .withScalaVersion(scalaVersion)
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

  def configuredSourceroot: Configured[AbsolutePath] = {
    val path = sourceroot.getOrElse(cwd)
    if (path.isDirectory) Configured.ok(path)
    else Configured.error(s"--sourceroot $path is not a directory")
  }

  def validatedClasspath: Classpath = {
    val targetroot = semanticdbOption("targetroot")
      .map(option => Classpath(option))
      .getOrElse(Classpath(Nil))
    val baseClasspath =
      if (autoClasspath && classpath.entries.isEmpty) {
        val roots =
          if (autoClasspathRoots.isEmpty) cwd :: Nil
          else autoClasspathRoots
        ClasspathOps.autoClasspath(roots)
      } else {
        classpath
      }
    baseClasspath ++ targetroot
  }

  def classLoader: ClassLoader =
    ClasspathOps.toOrphanClassLoader(validatedClasspath)

  def semanticdbOption(name: String): Option[String] = {
    val flag = s"-P:semanticdb:$name:"
    scalacOptions
      .filter(_.startsWith(flag))
      .lastOption
      .map(_.stripPrefix(flag))
  }

  def semanticdbFilterMatcher: FilterMatcher = {
    val include = semanticdbOption("include")
    val exclude = semanticdbOption("exclude")
    (include, exclude) match {
      case (None, None) => FilterMatcher.matchEverything
      case (Some(in), None) => FilterMatcher.include(in)
      case (None, Some(ex)) => FilterMatcher.exclude(ex)
      case (Some(in), Some(ex)) => FilterMatcher(List(in), List(ex))
    }
  }

  def configuredGlobal: Configured[LazyValue[Option[Global]]] =
    Configured.ok {
      val settings = new Settings()
      settings.YpresentationAnyThread.value = true
      settings.classpath.value = validatedClasspath.syntax
      val reporter = new ConsoleReporter(settings)
      LazyValue.fromUnsafe(() => new Global(settings, reporter))
    }

  def validate: Configured[ValidatedArgs] = {
    baseConfig.andThen {
      case (base, scalafixConfig, delegator) =>
        (
          configuredSourceroot |@|
            configuredSymtab |@|
            configuredRules(base, scalafixConfig) |@|
            resolvedPathReplace |@|
            configuredDiffDisable |@|
            configuredGlobal
        ).map {
          case (
              ((((root, symtab), rulez), pathReplace), diffDisable),
              global
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
              semanticdbFilterMatcher,
              global
            )
        }
    }
  }
}

object Args {
  val baseMatcher: PathMatcher =
    FileSystems.getDefault.getPathMatcher("glob:**.{scala,sbt}")
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
  implicit val callbackEncoder: ConfEncoder[ScalafixMainCallback] =
    ConfEncoder.StringEncoder.contramap(_.toString)

  implicit val argsEncoder: ConfEncoder[Args] = generic.deriveEncoder
  implicit val absolutePathPrint: TPrint[AbsolutePath] =
    TPrint.make[AbsolutePath](_ => "<path>")
  implicit val pathMatcherPrint: TPrint[PathMatcher] =
    TPrint.make[PathMatcher](_ => "<glob>")
  implicit val confPrint: TPrint[Conf] =
    TPrint.make[Conf](implicit cfg => TPrint.implicitly[ScalafixConfig].render)
  implicit def optionPrint[T](
      implicit ev: pprint.TPrint[T]
  ): TPrint[Option[T]] =
    TPrint.make { implicit cfg =>
      ev.render
    }
  implicit def iterablePrint[C[x] <: Iterable[x], T](
      implicit ev: pprint.TPrint[T]
  ): TPrint[C[T]] =
    TPrint.make { implicit cfg =>
      s"[${ev.render} ...]"
    }
  implicit val argsSurface: Surface[Args] = generic.deriveSurface
}
