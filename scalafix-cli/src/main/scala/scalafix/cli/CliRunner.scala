package scalafix
package cli

import java.io.{File, OutputStreamWriter}
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.function.UnaryOperator
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import scala.meta._
import scala.meta.inputs.Input
import scala.meta.internal.inputs._
import scala.meta.io.AbsolutePath
import scala.meta.semanticdb.SemanticdbSbt
import scala.util.control.NonFatal
import scala.util.Try
import scalafix.internal.cli.CommonOptions
import scalafix.internal.cli.FixFile
import scalafix.internal.cli.ScalafixOptions
import scalafix.internal.cli.TermDisplay
import scalafix.internal.cli.WriteMode
import scalafix.internal.config.Class2Hocon
import scalafix.internal.config.FilterMatcher
import scalafix.internal.config.LazySemanticdbIndex
import scalafix.internal.config.MetaconfigPendingUpstream
import scalafix.internal.config.RuleKind
import scalafix.internal.config.ScalafixConfig
import scalafix.internal.diff.DiffDisable
import scalafix.internal.jgit.JGitDiff
import scalafix.internal.util.Failure
import scalafix.internal.util.EagerInMemorySemanticdbIndex
import scalafix.reflect.ScalafixReflect
import scalafix.syntax._
import metaconfig.Configured.Ok
import metaconfig._

sealed abstract case class CliRunner(
    sourceroot: AbsolutePath,
    cli: ScalafixOptions,
    config: ScalafixConfig,
    rule: Rule,
    inputs: Seq[FixFile],
    replacePath: AbsolutePath => AbsolutePath,
    diffDisable: DiffDisable
) {
  val sbtConfig: ScalafixConfig = config.copy(dialect = dialects.Sbt0137)
  val writeMode: WriteMode =
    if (cli.stdout) WriteMode.Stdout
    else if (cli.test) WriteMode.Test
    else WriteMode.WriteFile
  val common: CommonOptions = cli.common
  implicit val workingDirectory: AbsolutePath = common.workingPath

  def run(): ExitStatus = {
    val display = new TermDisplay(
      new OutputStreamWriter(
        if (cli.stdout) cli.common.err else cli.common.out),
      fallbackMode = true)
    if (inputs.length > 10) display.init()
    val msg = cli.projectIdPrefix + s"Running ${rule.name}"
    display.startTask(msg, common.workingDirectoryFile)
    display.taskLength(msg, inputs.length, 0)
    val exitCode = new AtomicReference(ExitStatus.Ok)
    val counter = new AtomicInteger()
    val inputsToFix =
      if (cli.singleThread) inputs
      else inputs.toVector.par
    inputsToFix.foreach { input =>
      val code = safeHandleInput(input)
      val progress = counter.incrementAndGet()
      exitCode.getAndUpdate(new UnaryOperator[ExitStatus] {
        override def apply(t: ExitStatus): ExitStatus =
          ExitStatus.merge(code, t)
      })
      display.taskProgress(msg, progress)
      code
    }
    val exit: ExitStatus = {
      val code = exitCode.get()
      if (config.lint.reporter.hasErrors) {
        ExitStatus.merge(ExitStatus.LinterError, code)
      } else code
    }
    display.completedTask(msg, exit == ExitStatus.Ok)
    display.stop()
    exit
  }

  // safeguard to verify that the original file contents have not changed since the
  // creation of the .semanticdb file. Without this check, we risk overwriting
  // the latest changes written by the user.
  private def isUpToDate(input: FixFile): Boolean =
    if (!input.toIO.exists() && cli.outTo.nonEmpty) true
    else {
      input.semanticFile match {
        case Some(Input.VirtualFile(_, contents)) =>
          val fileToWrite = scala.io.Source.fromFile(input.toIO)
          try fileToWrite.sameElements(contents.toCharArray.toIterator)
          finally fileToWrite.close()
        case _ =>
          true // No way to check for Input.File, see https://github.com/scalameta/scalameta/issues/886
      }
    }

  def unsafeHandleInput(input: FixFile): ExitStatus = {
    val inputConfig =
      if (input.original.label.endsWith(".sbt")) sbtConfig
      else config

    inputConfig.dialect(input.toParse).parse[Source] match {
      case parsers.Parsed.Error(pos, message, _) =>
        if (cli.quietParseErrors && !input.passedExplicitly) {
          // Ignore parse errors, useful for example when running scalafix on
          // millions of lines of code for experimentation.
          ExitStatus.Ok
        } else {
          common.err.println(pos.formatMessage("error", message))
          ExitStatus.ParseError
        }
      case parsers.Parsed.Success(tree) =>
        val ctx = RuleCtx(tree, config, diffDisable)
        val (fixed, messages) = rule.applyAndLint(ctx)

        messages.foreach { msg =>
          val category = msg.category.withConfig(config.lint)
          config.lint.reporter.handleMessage(
            msg.format(config.lint.explain),
            msg.position,
            category.severity.toSeverity
          )
        }

        writeMode match {
          case WriteMode.Stdout =>
            common.out.write(fixed.getBytes)
            ExitStatus.Ok
          case WriteMode.Test =>
            val isUnchanged =
              input.original.path.readAllBytes
                .sameElements(fixed.getBytes(input.original.charset))
            if (isUnchanged) ExitStatus.Ok
            else {
              val diff =
                Patch.unifiedDiff(
                  input.original,
                  Input.VirtualFile(
                    s"<expected fix from ${rule.name}>",
                    fixed
                  )
                )
              common.out.println(diff)
              ExitStatus.TestFailed
            }
          case WriteMode.WriteFile =>
            val outFile = replacePath(input.original.path)
            if (isUpToDate(input)) {
              Files.createDirectories(outFile.toNIO.getParent)
              Files.write(outFile.toNIO, fixed.getBytes(input.original.charset))
              ExitStatus.Ok
            } else {
              cli.diagnostic.error(
                s"Stale semanticdb for ${CliRunner.pretty(outFile)}, skipping rule. Please recompile.")
              if (cli.verbose) {
                val diff =
                  Patch.unifiedDiff(input.semanticFile.get, input.original)
                common.err.println(diff)
              }
              ExitStatus.StaleSemanticDB
            }
        }
    }
  }

  def safeHandleInput(input: FixFile): ExitStatus =
    try unsafeHandleInput(input)
    catch {
      case NonFatal(e) =>
        reportError(input.original.path, e, cli)
        e match {
          case _: Failure => ExitStatus.ScalafixError
          case _ => ExitStatus.UnexpectedError
        }
    }

  def reportError(
      path: AbsolutePath,
      cause: Throwable,
      options: ScalafixOptions): Unit = {
    config.reporter.error(s"Failed to fix $path")
    cause.setStackTrace(cause.getStackTrace.take(options.common.stackVerbosity))
    cause.printStackTrace(options.common.err)
  }
}

object CliRunner {
  private[scalafix] def pretty(path: AbsolutePath)(
      implicit cwd: AbsolutePath): String = {
    // toRelative should not throw exceptions, but it does, see https://github.com/scalameta/scalameta/issues/821
    Try(path.toRelative(cwd)).getOrElse(path).toString
  }

  /** Construct CliRunner with rule from ScalafixOptions. */
  def fromOptions(options: ScalafixOptions): Configured[CliRunner] =
    safeFromOptions(options, None)

  /** Construct CliRunner with custom rule. */
  def fromOptions(options: ScalafixOptions, rule: Rule): Configured[CliRunner] =
    safeFromOptions(options, Some(rule))

  private def safeFromOptions(
      options: ScalafixOptions,
      rule: Option[Rule]
  ): Configured[CliRunner] = {
    try {
      val builder = new CliRunner.Builder(options)
      rule.fold(
        builder.resolvedRule.andThen(rule =>
          unsafeFromOptions(options, rule, builder))
      )(r => unsafeFromOptions(options, r, builder))
    } catch {
      case NonFatal(e) =>
        ConfError.exception(e, options.common.stackVerbosity).notOk
    }
  }
  private def unsafeFromOptions(
      options: ScalafixOptions,
      rule: Rule,
      builder: Builder
  ): Configured[CliRunner] = {
    (
      builder.resolvedSourceroot |@|
        builder.resolvedPathReplace |@|
        builder.fixFilesWithSemanticdbIndex |@|
        builder.resolvedConfig |@|
        builder.diffDisable
    ).map {
      case ((((sourceroot, replace), inputs), config), diffDisable) =>
        if (options.verbose) {
          options.diagnostic.info(
            s"""|Config:
                |${Class2Hocon(config)}
                |Rule:
                |$rule
                |""".stripMargin
          )
        }
        new CliRunner(
          sourceroot = sourceroot,
          cli = options,
          config = config,
          rule = rule,
          replacePath = replace,
          inputs = inputs,
          diffDisable = diffDisable
        ) {}
    }
  }
  private val META_INF = Paths.get("META-INF")
  private val SEMANTICDB = Paths.get("semanticdb")

  private def isTargetroot(path: Path): Boolean = {
    path.toFile.isDirectory &&
    path.resolve(META_INF).toFile.isDirectory &&
    path.resolve(META_INF).resolve(SEMANTICDB).toFile.isDirectory
  }

  def autoClasspath(roots: List[AbsolutePath]): Classpath = {
    val buffer = List.newBuilder[AbsolutePath]
    val visitor = new SimpleFileVisitor[Path] {
      override def preVisitDirectory(
          dir: Path,
          attrs: BasicFileAttributes): FileVisitResult = {
        if (isTargetroot(dir)) {
          buffer += AbsolutePath(dir)
          FileVisitResult.SKIP_SUBTREE
        } else {
          FileVisitResult.CONTINUE
        }
      }
    }
    roots.foreach(x => Files.walkFileTree(x.toNIO, visitor))
    Classpath(buffer.result())
  }

  private class Builder(val cli: ScalafixOptions) {
    import cli._

    implicit val workingDirectory: AbsolutePath = common.workingPath

    def toClasspath(cp: String): List[AbsolutePath] =
      cp.split(File.pathSeparator)
        .iterator
        .map(path => AbsolutePath(path)(common.workingPath))
        .toList

    def resolveClasspath: Configured[Classpath] =
      classpath match {
        case Some(cp) =>
          val paths = toClasspath(cp)
          Ok(Classpath(paths))
        case None =>
          val roots = cli.classpathAutoRoots.fold(
            cli.common.workingPath :: Nil)(toClasspath)
          val cp = autoClasspath(roots)
          if (verbose) {
            common.err.println(s"Automatic classpath=$cp")
          }
          if (cp.shallow.nonEmpty) Ok(cp)
          else {
            ConfError
              .msg(
                "Unable to infer --classpath containing .semanticdb files. " +
                  "Is the semanticdb compiler plugin installed?")
              .notOk
          }
      }

    val resolvedSourceroot: Configured[AbsolutePath] = {
      val result = sourceroot
        .map(AbsolutePath(_))
        .getOrElse(common.workingPath)
      if (result.isDirectory) Ok(result)
      else {
        ConfError.msg(s"Invalid --sourceroot $result is not a directory!").notOk
      }
    }

    // We don't know yet if we need to compute the database or not.
    // If all the rules are syntactic, we never need to compute the index.
    // If a single rule is semantic, then we need to compute the database.
    private var cachedDatabase = Option.empty[Configured[SemanticdbIndex]]
    private def computeAndCacheDatabase(): Option[SemanticdbIndex] = {
      val result: Configured[SemanticdbIndex] = cachedDatabase.getOrElse {
        (resolveClasspath |@| resolvedSourceroot).andThen {
          case (classpath, root) =>
            val patched = SemanticdbSbt.patchDatabase(
              Database.load(classpath, Sourcepath(root)),
              root)
            val db =
              EagerInMemorySemanticdbIndex(patched, Sourcepath(root), classpath)
            if (verbose) {
              common.err.println(
                s"Loaded database with ${db.documents.length} documents.")
            }
            if (db.documents.nonEmpty) Ok(db)
            else {
              ConfError
                .msg("Missing SemanticdbIndex, found no semanticdb files!")
                .notOk
            }
        }
      }
      if (cachedDatabase.isEmpty) {
        cachedDatabase = Some(result)
      }
      Some(MetaconfigPendingUpstream.get_!(result))
    }
    private def resolveDatabase(kind: RuleKind): Option[SemanticdbIndex] = {
      if (kind.isSyntactic) None
      else computeAndCacheDatabase()
    }
    private val lazySemanticdbIndex: LazySemanticdbIndex =
      new LazySemanticdbIndex(
        resolveDatabase,
        diagnostic,
        cli.common.workingPath,
        cli.toolClasspath.map(toClasspath).getOrElse(Nil))

    // expands a single file into a list of files.
    def expand(matcher: FilterMatcher)(path: AbsolutePath): Seq[FixFile] = {
      if (!path.toFile.exists()) {
        common.cliArg.error(s"$path does not exist. ${common.workingDirectory}")
        Nil
      } else if (path.isDirectory) {
        val builder = Seq.newBuilder[FixFile]
        Files.walkFileTree(
          path.toNIO,
          new SimpleFileVisitor[Path] {
            override def visitFile(
                file: Path,
                attrs: BasicFileAttributes): FileVisitResult = {
              if (Cli.isScalaPath(file) && matcher.matches(file.toString)) {
                builder += FixFile(Input.File(AbsolutePath(file)))
              }
              FileVisitResult.CONTINUE
            }
          }
        )
        builder.result()
      } else if (matcher.matches(path.toString())) {
        // if the user provided explicit path, take it.
        List(FixFile(Input.File(path), passedExplicitly = true))
      } else {
        Nil
      }
    }

    private val resolvedPathMatcher: Configured[FilterMatcher] = try {
      val include = if (files.isEmpty) List(".*") else files
      Ok(FilterMatcher(include, exclude))
    } catch {
      case e: PatternSyntaxException =>
        ConfError
          .msg(
            s"Invalid '${e.getPattern}' for  --include/--exclude. ${e.getMessage}")
          .notOk
    }

    val diffDisable: Configured[DiffDisable] = {
      if (cli.diff || cli.diffBase.nonEmpty) {
        val diffBase = cli.diffBase.getOrElse("master")
        JGitDiff(common.workingPath.toNIO, diffBase)
      } else {
        Configured.Ok(DiffDisable.empty)
      }
    }

    val fixFiles: Configured[Seq[FixFile]] = diffDisable.andThen(diffDisable0 =>
      resolvedPathMatcher.andThen { pathMatcher =>
        val paths =
          if (cli.files.nonEmpty) cli.files.map(AbsolutePath(_))
          // If no files are provided, assume cwd.
          else common.workingPath :: Nil
        val allFiles = paths.toVector.flatMap(expand(pathMatcher))

        val allFilesExcludingDiffs =
          allFiles.filterNot(fixFile =>
            diffDisable0.isDisabled(fixFile.original))

        if (allFilesExcludingDiffs.isEmpty) {
          ConfError
            .msg(
              s"No files to fix! Missing at least one .scala or .sbt file from: " +
                paths.mkString(", "))
            .notOk
        } else Ok(allFilesExcludingDiffs)
    })

    val resolvedConfigInput: Configured[Input] =
      (config, configStr) match {
        case (Some(x), Some(y)) =>
          ConfError
            .msg(s"Can't configure both --config $x and --config-str $y")
            .notOk
        case (Some(configPath), _) =>
          val path = AbsolutePath(configPath)
          if (path.isFile) Ok(Input.File(path))
          else ConfError.msg(s"--config $path is not a file").notOk
        case (_, Some(configString)) =>
          Ok(Input.String(configString))
        case _ =>
          Ok(
            ScalafixConfig
              .auto(common.workingPath)
              .getOrElse(Input.String("")))
      }

    // ScalafixConfig and Rule
    // Determines the config in the following order:
    //  - --config-str or --config
    //  - .scalafix.conf in working directory
    //  - ScalafixConfig.default
    val resolvedRuleAndConfig: Configured[(Rule, ScalafixConfig)] = {
      val decoder = ScalafixReflect.fromLazySemanticdbIndex(lazySemanticdbIndex)
      fixFiles.andThen { inputs =>
        val configured = resolvedConfigInput.andThen(input =>
          ScalafixConfig.fromInput(input, lazySemanticdbIndex, rules)(decoder))
        configured.map { configuration =>
          // TODO(olafur) implement withFilter on Configured
          val (finalRule, scalafixConfig) = configuration
          val finalConfig = scalafixConfig.withOut(common.err)
          finalRule -> finalConfig
        }
      }
    }
    val resolvedRule: Configured[Rule] =
      resolvedRuleAndConfig.andThen {
        case (rule, _) =>
          if (rule.name.isEmpty)
            ConfError
              .msg("No rule was provided! Use --rule to specify a rule.")
              .notOk
          else Ok(rule)
      }
    val resolvedConfig: Configured[ScalafixConfig] =
      resolvedRuleAndConfig.map {
        case (_, config) => config.withFreshReporters(common.out)
      }

    val resolvedPathReplace: Configured[AbsolutePath => AbsolutePath] = try {
      (outFrom, outTo) match {
        case (None, None) => Ok(identity[AbsolutePath])
        case (Some(from), Some(to)) =>
          val outFromPattern = Pattern.compile(from)
          def replacePath(file: AbsolutePath): AbsolutePath =
            AbsolutePath(outFromPattern.matcher(file.toString()).replaceAll(to))
          Ok(replacePath _)
        case (Some(from), _) =>
          ConfError
            .msg(s"--out-from $from must be accompanied with --out-to")
            .notOk
        case (_, Some(to)) =>
          ConfError
            .msg(s"--out-to $to must be accompanied with --out-from")
            .notOk
      }
    } catch {
      case e: PatternSyntaxException =>
        ConfError.msg(s"Invalid regex '$outFrom'! ${e.getMessage}").notOk
    }

    val semanticInputs: Configured[Map[AbsolutePath, Input.VirtualFile]] = {
      (resolvedRule |@| resolvedSourceroot).andThen {
        case (_, root) =>
          cachedDatabase.getOrElse(Ok(SemanticdbIndex.empty)).map { database =>
            def checkExists(path: AbsolutePath): Unit =
              if (!cli.noStrictSemanticdb && !path.isFile) {
                common.cliArg.error(
                  s"semanticdb input $path is not a file. Is --sourceroot correct?")
              }
            val inputsByAbsolutePath =
              database.documents.toIterator.map(_.input).collect {
                case input @ Input.VirtualFile(path, _) =>
                  val key = root.resolve(path)
                  checkExists(key)
                  key -> input
                case input @ Input.File(path, _) =>
                  // Some semanticdbs may have Input.File, for example in semanticdb-sbt.
                  checkExists(path)
                  val contents = new String(input.chars)
                  path -> Input.VirtualFile(path.toString(), contents)
              }
            inputsByAbsolutePath.toMap
          }
      }
    }

    val fixFilesWithSemanticdbIndex: Configured[Seq[FixFile]] =
      (semanticInputs |@| fixFiles).map {
        case (fromSemanticdbIndex, files) =>
          files.map { file =>
            val labeled = fromSemanticdbIndex.get(file.original.path)
            if (!cli.noStrictSemanticdb && fromSemanticdbIndex.nonEmpty && labeled.isEmpty) {
              common.cliArg.error(
                s"No semanticdb associated with ${file.original.path}. " +
                  s"Is --sourceroot correct?")
            }
            file.copy(semanticFile = labeled)
          }
      }
  }
}
