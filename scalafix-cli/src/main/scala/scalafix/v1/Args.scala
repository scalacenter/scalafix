package scalafix.v1

import scala.language.higherKinds
import java.io.File
import java.io.PrintStream
import java.net.URI
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.nio.file.PathMatcher
import java.nio.file.Paths
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import metaconfig.Configured._
import metaconfig._
import metaconfig.annotation.Description
import metaconfig.annotation.ExtraName
import metaconfig.generic.Surface
import metaconfig.internal.ConfGet
import metaconfig.typesafeconfig.typesafeConfigMetaconfigParser
import pprint.TPrint
import scala.annotation.StaticAnnotation
import scala.meta.internal.io.PathIO
import scala.meta.io.AbsolutePath
import scala.meta.io.Classpath
import scalafix.cli.CliRunner
import scalafix.internal.config.OutputFormat
import scalafix.internal.config.ScalafixConfig
import scalafix.internal.diff.DiffDisable
import scalafix.internal.jgit.JGitDiff
import scalafix.internal.reflect.ClasspathOps
import scalafix.internal.util.SymbolTable
import scalafix.internal.v1.Rules

class Section(val name: String) extends StaticAnnotation

case class Args(
    @Section("Common options")
    @Description(
      "Scalafix rules to run, for example ExplicitResultTypes. " +
        "The syntax for rules is documented in https://scalacenter.github.io/scalafix/docs/users/configuration#rules")
    @ExtraName("r")
    rules: List[String] = Nil,
    @Description("Files or directories (recursively visited) to fix.")
    @ExtraName("remainingArgs")
    files: List[AbsolutePath] = Nil,
    @Description(
      "File path to a .scalafix.conf configuration file. " +
        "Defaults to .scalafix.conf in the current working directory, if any.")
    config: Option[AbsolutePath] = None,
    @Description(
      "Check that all files have been fixed with scalafix, exiting with non-zero code on violations. " +
        "Won't write to files.")
    test: Boolean = false,
    @Description("Print fixed output to stdout instead of writing in-place.")
    stdout: Boolean = false,
    @Description(
      "If set, only apply scalafix to added and edited files in git diff against the master branch.")
    diff: Boolean = false,
    @Description(
      "If set, only apply scalafix to added and edited files in git diff against a provided branch, commit or tag.")
    diffBase: Option[String] = None,
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
    @Description("Absolute path passed to semanticdb with -P:semanticdb:sourceroot:<path>. " +
      "Relative filenames persisted in the Semantic DB are absolutized by the " +
      "sourceroot. Defaults to current working directory if not provided.")
    sourceroot: Option[AbsolutePath] = None,
    @Description(
      "Global cache location to persist metacp artifacts produced by analyzing --dependency-classpath. " +
        "The default location depends on the OS and is computed with https://github.com/soc/directories-jvm " +
        "using the project name 'semanticdb'. " +
        "On macOS the default cache directory is ~/Library/Caches/semanticdb. ")
    metacpCacheDir: Option[AbsolutePath] = None,
    @Description(
      "If set, automatically infer the --classpath flag by scanning for directories with META-INF/semanticdb")
    autoClasspath: Boolean = false,
    @Description("Additional directories to scan for --auto-classpath")
    @ExtraName("classpathAutoRoots")
    autoClasspathRoots: List[AbsolutePath] = Nil,
    @Section("Less common options")
    @Description(
      "Unix-style glob for files to exclude from fixing. " +
        "The glob syntax is defined by `nio.FileSystem.getPathMatcher`.")
    exclude: List[PathMatcher] = Nil,
    @Description(
      "Additional classpath for compiling and classloading custom rules.")
    toolClasspath: Classpath = Classpath(Nil),
    @Description("The encoding to use for reading/writing files")
    charset: Charset = StandardCharsets.UTF_8,
    @Description("If set, throw exception in the end instead of System.exit")
    noSysExit: Boolean = false,
    @Description("Don't error on stale semanticdb files.")
    noStaleSemanticdb: Boolean = false,
    @Description("Custom settings to override .scalafix.conf")
    settings: Conf = Conf.Obj.empty,
    @Description(
      "The format for console output, if sbt prepends [error] prefix")
    format: OutputFormat = OutputFormat.Default,
    @Description(
      "Regex that is passed as first argument to fileToFix.replaceAll(outFrom, outTo)")
    outFrom: Option[String] = None,
    @Description(
      "Replacement string that is passed as second argument to fileToFix.replaceAll(outFrom, outTo)")
    outTo: Option[String] = None,
    @Description(
      "Write to files. In case of linter error adds a comment to suppress the error.")
    autoSuppressLinterErrors: Boolean = false,
    @Description("The current working directory")
    cwd: AbsolutePath,
    @Hidden
    out: PrintStream,
    @Hidden
    ls: Ls = Ls.Find
) {

  def configuredSymtab: Configured[SymbolTable] = {
    ClasspathOps.newSymbolTable(
      classpath = classpath,
      cacheDirectory = metacpCacheDir.headOption,
      out = out
    ) match {
      case Some(symtab) =>
        Configured.ok(symtab)
      case _ =>
        ConfError.message("Unable to load symbol table").notOk
    }
  }

  def baseConfig: Configured[(Conf, ScalafixConfig)] = {
    val toRead: Option[AbsolutePath] = config.orElse {
      val defaultPath = cwd.resolve(".scalafix.conf")
      if (defaultPath.isFile) Some(defaultPath)
      else None
    }
    val base = toRead match {
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
    base.andThen { b =>
      val applied = Conf.applyPatch(b, settings)
      applied.as[ScalafixConfig].map { scalafixConfig =>
        applied -> scalafixConfig.withOut(out)
      }
    }
  }

  def configuredRules(
      base: Conf,
      scalafixConfig: ScalafixConfig
  ): Configured[Rules] = {
    val rulesConf =
      if (rules.isEmpty) {
        ConfGet.getKey(base, "rules" :: "rule" :: Nil) match {
          case Some(c) => c
          case _ => Conf.Lst(Nil)
        }
      } else {
        Conf.Lst(rules.map(Conf.fromString))
      }
    val decoderSettings = RuleDecoder
      .Settings()
      .withConfig(scalafixConfig)
      .withToolClasspath(toolClasspath.entries)
      .withCwd(cwd)
    val decoder = RuleDecoder.decoder(decoderSettings)
    decoder.read(rulesConf).andThen(_.withConfig(base))
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
                  outFromPattern.matcher(file.toURI.getPath).replaceAll(to))
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
    if (autoClasspath && classpath.entries.isEmpty) {
      val roots =
        if (autoClasspathRoots.isEmpty) cwd :: Nil
        else autoClasspathRoots
      CliRunner.autoClasspath(roots)
    } else classpath
  }

  def validate: Configured[ValidatedArgs] = {
    baseConfig.andThen {
      case (base, scalafixConfig) =>
        (
          configuredSourceroot |@|
            configuredSymtab |@|
            configuredRules(base, scalafixConfig) |@|
            resolvedPathReplace |@|
            configuredDiffDisable
        ).map {
          case ((((root, symtab), rulez), pathReplace), diffDisable) =>
            ValidatedArgs(
              this,
              symtab,
              rulez,
              scalafixConfig.withFormat(
                format
              ),
              validatedClasspath,
              root,
              pathReplace,
              diffDisable
            )
        }
    }
  }
}

object Args {
  val baseMatcher: PathMatcher =
    FileSystems.getDefault.getPathMatcher("glob:**.{scala,sbt}")
  val default = new Args(cwd = PathIO.workingDirectory, out = System.out)

  def decoder(cwd: AbsolutePath, out: PrintStream): ConfDecoder[Args] = {
    implicit val classpathDecoder: ConfDecoder[Classpath] =
      ConfDecoder.stringConfDecoder.map { cp =>
        Classpath(
          cp.split(File.pathSeparator)
            .iterator
            .map(path => AbsolutePath(path)(cwd))
            .toList
        )
      }
    implicit val absolutePathDecoder: ConfDecoder[AbsolutePath] =
      ConfDecoder.stringConfDecoder.map(AbsolutePath(_)(cwd))
    val base = new Args(cwd = cwd, out = out)
    generic.deriveDecoder(base)
  }

  implicit val charsetDecoder: ConfDecoder[Charset] =
    ConfDecoder.stringConfDecoder.map(name => Charset.forName(name))
  implicit val printStreamDecoder: ConfDecoder[PrintStream] =
    ConfDecoder.stringConfDecoder.map(_ => System.out)
  implicit val pathMatcherDecoder: ConfDecoder[PathMatcher] =
    ConfDecoder.stringConfDecoder.map(glob =>
      FileSystems.getDefault.getPathMatcher("glob:" + glob))

  implicit val confEncoder: ConfEncoder[Conf] =
    ConfEncoder.ConfEncoder
  implicit val pathEncoder: ConfEncoder[AbsolutePath] =
    ConfEncoder.StringEncoder.contramap(_.toString())
  implicit val classpathEncoder: ConfEncoder[Classpath] =
    ConfEncoder.StringEncoder.contramap(_.toString())
  implicit val charsetEncoder: ConfEncoder[Charset] =
    ConfEncoder.StringEncoder.contramap(_.name())
  implicit val printStreamEncoder: ConfEncoder[PrintStream] =
    ConfEncoder.StringEncoder.contramap(_ => "<stdout>")
  implicit val pathMatcherEncoder: ConfEncoder[PathMatcher] =
    ConfEncoder.StringEncoder.contramap(_.toString)

  implicit val argsEncoder: ConfEncoder[Args] = generic.deriveEncoder
  implicit val absolutePathPrint: TPrint[AbsolutePath] =
    TPrint.make[AbsolutePath](_ => "<path>")
  implicit val pathMatcherPrint: TPrint[PathMatcher] =
    TPrint.make[PathMatcher](_ => "<glob>")
  implicit val confPrint: TPrint[Conf] =
    TPrint.make[Conf](implicit cfg => TPrint.implicitly[ScalafixConfig].render)
  implicit val outputFormat: TPrint[OutputFormat] =
    TPrint.make[OutputFormat](implicit cfg =>
      OutputFormat.all.map(_.toString.toLowerCase).mkString("<", "|", ">"))
  implicit def optionPrint[T](
      implicit ev: pprint.TPrint[T]): TPrint[Option[T]] =
    TPrint.make { implicit cfg =>
      ev.render
    }
  implicit def iterablePrint[C[x] <: Iterable[x], T](
      implicit ev: pprint.TPrint[T]): TPrint[C[T]] =
    TPrint.make { implicit cfg =>
      s"[${ev.render} ...]"
    }
  implicit val argsSurface: Surface[Args] = generic.deriveSurface
}

case class ScalafixFileConfig(rules: Conf, other: Conf)
object ScalafixFileConfig {
  val empty = ScalafixFileConfig(
    Conf.Obj.empty,
    Conf.Obj.empty
  )
}
