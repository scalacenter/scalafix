package scalafix.v1

import java.io.PrintStream
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import metaconfig.Configured
import scala.meta.internal.io.FileIO
import scala.meta.io.AbsolutePath
import scala.collection.mutable.ArrayBuffer
import scala.meta.parsers.Parsed
import scala.util.control.NonFatal
import scalafix.cli.ExitStatus
import scalafix.internal.cli.CliParser
import scalafix.internal.cli.WriteMode
import scalafix.internal.config.ScalafixReporter
import scalafix.lint.LintMessage

object Main {

  def files(args: ValidatedArgs): Seq[AbsolutePath] = args.args.ls match {
    case Ls.Find =>
      val buf = ArrayBuffer.empty[AbsolutePath]
      val visitor = new SimpleFileVisitor[Path] {
        override def visitFile(
            file: Path,
            attrs: BasicFileAttributes): FileVisitResult = {
          val path = AbsolutePath(file)
          val relpath = path.toRelative(args.sourceroot)
          if (args.matches(relpath)) {
            buf += path
          }
          FileVisitResult.CONTINUE
        }
      }

      val roots =
        if (args.args.files.isEmpty) args.sourceroot :: Nil
        else args.args.files

      roots.foreach { root =>
        if (root.isFile) {
          if (args.matches(root.toRelative(args.args.cwd))) {
            buf += root
          }
        } else if (root.isDirectory) Files.walkFileTree(root.toNIO, visitor)
        else args.config.reporter.error(s"$root is not a file")
      }
      buf.result()
  }

  def main(args: Array[String]): Unit = {
    run(args, AbsolutePath.workingDirectory.toNIO, System.out)
  }

  def handleException(ex: Throwable, out: PrintStream): Unit = {
    val st = ex.getStackTrace.filterNot { e =>
      e.getClassName.startsWith("java.lang.Thread") ||
      e.getClassName.startsWith("java.util.concurrent.") ||
      e.getClassName.startsWith("org.scalatest") ||
      e.getClassName.startsWith("sbt.")
    }
    ex.setStackTrace(st)
    ex.printStackTrace(out)
  }

  def adjustExitCode(
      args: ValidatedArgs,
      code: ExitStatus,
      files: Seq[AbsolutePath]
  ): ExitStatus = {
    if (args.config.lint.reporter.hasErrors) {
      ExitStatus.merge(ExitStatus.LinterError, code)
    } else if (args.config.reporter.hasErrors && code.isOk) {
      ExitStatus.merge(ExitStatus.UnexpectedError, code)
    } else if (files.isEmpty) {
      args.config.reporter.error("No files to fix")
      ExitStatus.merge(ExitStatus.NoFilesError, code)
    } else {
      code
    }
  }

  def reportLintErrors(args: ValidatedArgs, messages: List[LintMessage]): Unit =
    messages.foreach { msg =>
      val category = msg.category.withConfig(args.config.lint)
      args.config.lint.reporter.handleMessage(
        msg.format(args.config.lint.explain),
        msg.position,
        category.severity.toSeverity
      )
    }

  def unsafeHandleFile(args: ValidatedArgs, file: AbsolutePath): ExitStatus = {
    val input = args.input(file)
    args.parse(input) match {
      case Parsed.Error(_, _, ex) =>
        handleException(ex, args.args.out)
        ExitStatus.ParseError
      case Parsed.Success(tree) =>
        val doc = Doc(tree, args.diffDisable, args.config)
        val (fixed, messages) =
          if (args.rules.isSemantic) {
            val relpath = file.toRelative(args.sourceroot)
            val sdoc = SemanticDoc.fromPath(
              doc,
              relpath,
              args.classpath,
              args.symtab
            )
            args.rules.semanticPatch(sdoc, args.args.autoSuppressLinterErrors)
          } else {
            args.rules.syntacticPatch(doc, args.args.autoSuppressLinterErrors)
          }
        if (!args.args.autoSuppressLinterErrors) {
          reportLintErrors(args, messages)
        }
        args.mode match {
          case WriteMode.Test =>
            if (fixed == input.text) ExitStatus.Ok
            else ExitStatus.TestFailed
          case WriteMode.Stdout =>
            args.args.out.println(fixed)
            ExitStatus.Ok
          case WriteMode.WriteFile =>
            if (fixed != input.text) {
              val toFix = args.pathReplace(file).toNIO
              Files.createDirectories(toFix.getParent)
              Files.write(toFix, fixed.getBytes(args.args.charset))
            }
            ExitStatus.Ok
        }
    }
  }

  def handleFile(args: ValidatedArgs, file: AbsolutePath): ExitStatus = {
    try unsafeHandleFile(args, file)
    catch {
      case e: SemanticDoc.Error.MissingSemanticdb =>
        args.config.reporter.error(e.getMessage)
        ExitStatus.MissingSemanticDB
      case NonFatal(e) =>
        handleException(e, args.args.out)
        ExitStatus.UnexpectedError
    }
  }

  def run(args: ValidatedArgs): ExitStatus = {
    val files = this.files(args)
    var exit = ExitStatus.Ok
    files.foreach { file =>
      val next = handleFile(args, file)
      exit = ExitStatus.merge(exit, next)
    }
    adjustExitCode(args, exit, files)
  }

  def run(args: Seq[String], cwd: Path, out: PrintStream): ExitStatus =
    CliParser
      .parseArgs[Args](args.toList)
      .andThen { c =>
        c.as[Args](Args.decoder(AbsolutePath(cwd), out))
      }
      .andThen(_.validate) match {
      case Configured.Ok(validated) =>
        if (validated.rules.isEmpty) {
          out.println("Missing --rules")
          ExitStatus.InvalidCommandLineOption
        } else {
          val adjusted = validated.copy(
            args = validated.args.copy(
              out = out,
              cwd = AbsolutePath(cwd)
            )
          )
          run(adjusted)
        }
      case Configured.NotOk(err) =>
        ScalafixReporter.default.copy(outStream = out).error(err.toString())
        ExitStatus.InvalidCommandLineOption
    }
}
