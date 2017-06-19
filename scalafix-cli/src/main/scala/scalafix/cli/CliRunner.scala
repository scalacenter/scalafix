package scalafix
package cli

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.URI
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitor
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Predicate
import java.util.function.UnaryOperator
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import java.util.stream.Collectors
import scala.annotation.tailrec
import scala.collection.immutable.Seq
import scala.collection.mutable.ListBuffer
import scala.meta._
import scala.meta.internal.inputs._
import scala.meta.internal.io.FileIO
import scala.meta.internal.io.PathIO
import scala.meta.internal.semantic.vfs
import scala.meta.parsers.ParseException
import scala.util.Success
import scala.util.Try
import scala.util.control.NonFatal
import scalafix.cli.termdisplay.TermDisplay
import scalafix.config.Class2Hocon
import scalafix.config.FilterMatcher
import scalafix.config.MetaconfigPendingUpstream._
import scalafix.config.PrintStreamReporter
import scalafix.config.ScalafixConfig
import scalafix.config.ScalafixReporter
import scalafix.reflect.ScalafixCompilerDecoder
import scalafix.reflect.ScalafixToolbox
import scalafix.rewrite.ScalafixRewrites
import scalafix.syntax._
import metaconfig.Configured.NotOk
import metaconfig.Configured.Ok
import metaconfig._
import org.scalameta.logger

sealed abstract case class CliRunner(
    sourceroot: AbsolutePath,
    cli: ScalafixOptions,
    config: ScalafixConfig,
    database: Option[Database],
    rewrite: Rewrite,
    explicitPaths: scala.Seq[Input],
    inputs: scala.Seq[Input],
    replacePath: AbsolutePath => AbsolutePath
) {
  val sbtConfig: ScalafixConfig = config.copy(dialect = dialects.Sbt0137)
  val writeMode: WriteMode =
    if (cli.stdout) WriteMode.Stdout
    else WriteMode.WriteFile
  val common: CommonOptions = cli.common
  val explicitURIs: Set[URI] =
    explicitPaths.toIterator
      .map(x => new URI(x.path(common.workingPath).toString()).normalize())
      .toSet

  def reportParseError(path: AbsolutePath): Boolean =
    cli.quietParseErrors &&
      explicitURIs.contains(path.toURI.normalize())

  def run(): ExitStatus = {
    val display = new TermDisplay(new OutputStreamWriter(System.out))
    if (inputs.length > 10) display.init()
    if (inputs.isEmpty) common.err.println("Running scalafix on 0 files.")
    val msg = s"Running scalafix rewrite ${rewrite.name}..."
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
    display.stop()
    exitCode.get()
  }

  // checks if outFile contents have changed since the creation its .semanticdb file.
  private def isUpToDate(input: Input, outFile: File): Boolean =
    if (database.isEmpty) true
    else if (!outFile.exists() && cli.outTo.nonEmpty) true
    else {
      input match {
        case Input.LabeledString(_, _) =>
          val fileToWrite = scala.io.Source.fromFile(outFile)
          val originalContents = input.chars.toIterator
          try {
            @tailrec
            def isEqual(a: Iterator[Char], b: Iterator[Char]): Boolean =
              if (a.hasNext && b.hasNext) a.next() == b.next() && isEqual(a, b)
              else if (a.hasNext || b.hasNext) false
              else true
            isEqual(fileToWrite, originalContents)
          } finally fileToWrite.close()
        case _ =>
          true // No way to check for Input.File, see https://github.com/scalameta/scalameta/issues/886
      }
    }

  def safeHandleInput(input: Input): ExitStatus = {
    def path = input.path(sourceroot)
    try {
      val inputConfig = if (input.isSbtFile) sbtConfig else config
      inputConfig.dialect(input).parse[Source] match {
        case parsers.Parsed.Error(pos, message, _) =>
          if (reportParseError(path)) {
            common.err.println(pos.formatMessage("error", message))
            ExitStatus.ParseError
          } else {
            // Ignore parse errors, useful for example when running scalafix on
            // millions of lines of code for experimentation.
            ExitStatus.Ok
          }
        case parsers.Parsed.Success(tree) =>
          val ctx = RewriteCtx(tree, config)
          val fixed = rewrite(ctx)
          if (writeMode.isWriteFile) {
            val outFile = replacePath(path)
            if (isUpToDate(input, outFile.toFile)) {
              Files.write(outFile.toNIO, fixed.getBytes(input.charset))
              ExitStatus.Ok
            } else {
              val relpath =
                // toRelative should not throw exceptions, but it does, see https://github.com/scalameta/scalameta/issues/821
                Try(outFile.toRelative(common.workingPath)).getOrElse(outFile)
              ctx.reporter.error(
                s"Stale semanticdb for $relpath, skipping rewrite. Pease recompile.")
              ExitStatus.StaleSemanticDB
            }
          } else {
            common.out.write(fixed.getBytes)
            ExitStatus.Ok
          }
      }
    } catch {
      case NonFatal(e) =>
        reportError(path, e, cli)
        e match {
          case _: scalafix.Failure => ExitStatus.ScalafixError
          case _ => ExitStatus.UnexpectedError
        }
    }
  }

  def reportError(path: AbsolutePath,
                  cause: Throwable,
                  options: ScalafixOptions): Unit = {
    options.common.reporter.error(s"Failed to fix $path")
    cause.setStackTrace(
      cause.getStackTrace.take(options.common.stackVerbosity))
    cause.printStackTrace(options.common.err)
  }
}

object CliRunner {
  def fromOptions(options: ScalafixOptions): Configured[CliRunner] = {
    val builder = new CliRunner.Builder(options)
    for {
      database <- builder.resolvedMirror
      sourceroot <- builder.resolvedSourceroot
      rewrite <- builder.resolvedRewrite
      replace <- builder.resolvedPathReplace
      inputs <- builder.resolvedInputs
      config <- builder.resolvedConfig
    } yield {
      if (options.verbose) {
        val entries = database.map(_.entries.length).getOrElse(0)
        val inputsNames = inputs
          .map(_.syntax.stripPrefix(options.common.workingDirectory + "/"))
          .mkString(", ")
        options.common.err.println(
          s"""|Files to fix (${inputs.length}):
              |$inputsNames
              |Database entires count: $entries
              |Database head:
              |${database.toString.take(1000)}
              |Config:
              |${Class2Hocon(config)}
              |Rewrite:
              |$rewrite
              |""".stripMargin
        )
      }
      new CliRunner(
        sourceroot = sourceroot,
        cli = options,
        config = config,
        database = database,
        rewrite = rewrite,
        replacePath = replace,
        inputs = inputs,
        explicitPaths = builder.explicitPaths
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
  def autoClasspath(workingPath: AbsolutePath): Classpath = {
    val buffer = List.newBuilder[AbsolutePath]
    Files.walkFileTree(
      workingPath.toNIO,
      new SimpleFileVisitor[Path] {
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
    )
    Classpath(buffer.result())
  }

  private class Builder(val cli: ScalafixOptions) {
    import cli._

    implicit val workingDirectory: AbsolutePath = common.workingPath

    val resolvedClasspath: Option[Configured[Classpath]] =
      if (autoMirror) {
        val cp = autoClasspath(common.workingPath)
        if (verbose) {
          common.err.println(s"Automatic classpath=$cp")
        }
        if (cp.shallow.nonEmpty) Some(Ok(cp))
        else {
          val msg =
            """Unable to automatically detect .semanticdb files to run semantic rewrites. Possible workarounds:
              |- re-compile sources with the scalahost compiler plugin enabled.
              |- pass in the --syntactic flag to run only syntactic rewrites.
              |- explicitly pass in --classuath and --sourceroot to run semantic rewrites.""".stripMargin
          Some(ConfError.msg(msg).notOk)
        }
      } else {
        classpath.map { x =>
          val paths = x.split(File.pathSeparator).map { path =>
            AbsolutePath.fromString(path)(common.workingPath)
          }
          Ok(Classpath(paths))
        }
      }

    val autoSourceroot: Option[AbsolutePath] =
      if (autoMirror) resolvedClasspath.map(_ => common.workingPath)
      else sourceroot.map(AbsolutePath.fromString(_))

    // Database
    private val resolvedDatabase: Configured[Database] =
      (resolvedClasspath, autoSourceroot) match {
        case (Some(Ok(cp)), sp) =>
          val tryMirror = for {
            mirror <- Try {
              val sourcepath = sp.map(Sourcepath.apply)
              val mirror =
                vfs.Database.load(cp).toSchema.toMeta(sourcepath)
              mirror
            }
          } yield mirror
          tryMirror match {
            case Success(x) => Ok(x)
            case scala.util.Failure(e) => ConfError.msg(e.toString).notOk
          }
        case (Some(err @ NotOk(_)), _) => err
        case (None, Some(sp)) =>
          ConfError
            .msg(
              s"Missing --classpath, cannot use --sourcepath $sp without --classpath")
            .notOk
        case (None, None) =>
          Ok(ScalafixRewrites.emptyDatabase)
      }
    val resolvedMirror: Configured[Option[Database]] =
      resolvedDatabase.map { x =>
        if (x == ScalafixRewrites.emptyDatabase) None
        else Some(x)
      }
    val resolvedMirrorSourceroot: Configured[AbsolutePath] =
      autoSourceroot match {
        case None => ConfError.msg("--sourceroot is required").notOk
        case Some(path) => Ok(path)
      }
    val resolvedSourceroot: Configured[AbsolutePath] =
      resolvedMirror.flatMap {
        // Use mirror sourceroot if Mirror.nonEmpty
        case Some(_) => resolvedMirrorSourceroot
        // Use working directory if running syntactic rewrites.
        case None => Ok(common.workingPath)
      }

    // Inputs
    val explicitPaths: scala.Seq[Input] =
      if (syntactic) {
        cli.files.toVector.flatMap { file =>
          val path = AbsolutePath.fromString(file)
          if (path.isDirectory) {
            import scala.collection.JavaConverters._
            val x = Files
              .walk(path.toNIO)
              .filter(new Predicate[Path] {
                override def test(t: Path): Boolean =
                  t.getFileName.toString.endsWith(".scala")
              })
              .collect(Collectors.toList[Path])
            x.asScala.toIterator
              .map(path => Input.File(AbsolutePath(path)))
              .toSeq
          } else {
            // if the user provided explicit path, take it.
            List(Input.File(path))
          }
        }
      } else Nil
    def isInputFile(input: Input): Boolean = input match {
      case _: Input.File | _: Input.LabeledString => true
      case _ => false
    }

    val mirrorPaths: Configured[Seq[Input]] =
      resolvedDatabase.flatMap { database =>
        val nonInputFiles = database.entries.filter(x => !isInputFile(x._1))
        if (nonInputFiles.isEmpty) Ok(database.entries.map(_._1))
        else {
          ConfError
            .msg(
              s"""Expected Input.File from parsed database, received:
                 |${nonInputFiles.mkString("\n")}""".stripMargin
            )
            .notOk
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
    val resolvedInputs: Configured[Seq[Input]] = for {
      fromMirror <- mirrorPaths
      pathMatcher <- resolvedPathMatcher
      sourceroot <- resolvedSourceroot
    } yield {
      def inputOK(input: Input) = pathMatcher.matches(input.label)
      val builder = Seq.newBuilder[Input]
      fromMirror.withFilter(inputOK).foreach(builder += _)
      explicitPaths.withFilter(inputOK).foreach(builder += _)
      builder.result()
    }

    lazy val resolvedConfigInput: Configured[Input] =
      (config, configStr) match {
        case (Some(x), Some(y)) =>
          ConfError
            .msg(s"Can't configure both --config $x and --config-str $y")
            .notOk
        case (Some(configPath), _) =>
          val path = AbsolutePath.fromString(configPath)
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

    // ScalafixConfig and Rewrite
    // Determines the config in the following order:
    //  - --config-str or --config
    //  - .scalafix.conf in working directory
    //  - ScalafixConfig.default
    val resolvedRewriteAndConfig: Configured[(Rewrite, ScalafixConfig)] =
      for {
        mirror <- resolvedMirror
        decoder = ScalafixCompilerDecoder.fromMirrorOption(mirror)
        cliArgRewrite <- rewrites
          .foldLeft(ScalafixToolbox.emptyRewrite) {
            case (rewrite, next) =>
              rewrite
                .product(decoder.read(Conf.Str(next)))
                .map { case (a, b) => a.andThen(b) }
          }
        inputs <- resolvedInputs
        configuration <- {
          if (inputs.isEmpty) {
            Ok(Rewrite.empty -> ScalafixConfig.default)
          } else {
            resolvedConfigInput.flatMap(input =>
              ScalafixConfig.fromInput(input, mirror)(decoder))
          }
        }
        // TODO(olafur) implement withFilter on Configured
        (configRewrite, scalafixConfig) = configuration
        finalConfig = scalafixConfig.copy(
          reporter = scalafixConfig.reporter match {
            case r: PrintStreamReporter => r.copy(outStream = common.err)
            case _ => ScalafixReporter.default.copy(outStream = common.err)
          }
        )
        finalRewrite = cliArgRewrite.andThen(configRewrite)
      } yield finalRewrite -> finalConfig
    val resolvedRewrite: Configured[Rewrite] =
      resolvedRewriteAndConfig.map(_._1)
    val resolvedConfig: Configured[ScalafixConfig] =
      resolvedRewriteAndConfig.map(_._2)

    val resolvedPathReplace: Configured[AbsolutePath => AbsolutePath] = try {
      val outFromPattern = Pattern.compile(outFrom.getOrElse(""))
      def replacePath(file: AbsolutePath): AbsolutePath =
        AbsolutePath(
          outFromPattern
            .matcher(file.toString())
            .replaceAll(outTo.getOrElse("")))
      Ok(replacePath _)
    } catch {
      case e: PatternSyntaxException =>
        ConfError.msg(s"Invalid regex '$outFrom'! ${e.getMessage}").notOk
    }
  }
}
