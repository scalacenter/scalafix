package scalafix.v1

import java.io.PrintStream
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import metaconfig.Conf
import metaconfig.Configured
import org.langmeta.io.AbsolutePath
import scala.collection.mutable.ArrayBuffer
import scala.meta.parsers.Parsed
import scala.util.control.NonFatal
import scalafix.cli.ExitStatus
import scalafix.internal.cli.ClasspathOps
import scalafix.internal.cli.CliParser
import scalafix.internal.cli.WriteMode
import scalafix.internal.util.SymbolTable

object Main {

  def files(args: ValidatedArgs): Seq[AbsolutePath] = args.args.ls match {
    case Ls.Find =>
      val buf = ArrayBuffer.empty[AbsolutePath]
      Files.walkFileTree(
        args.args.cwd.toNIO,
        new SimpleFileVisitor[Path] {
          override def visitFile(
              file: Path,
              attrs: BasicFileAttributes): FileVisitResult = {
            val path = AbsolutePath(file)
            if (args.matches(path)) {
              buf += path
            }
            FileVisitResult.CONTINUE
          }
        }
      )
      buf.result()
  }

  def main(args: Array[String]): Unit = {
    run(args, AbsolutePath.workingDirectory.toNIO, System.out)
  }

  def unsafeHandleFile(args: ValidatedArgs, file: AbsolutePath): ExitStatus = {
    val input = args.input(file)
    args.parse(input) match {
      case Parsed.Error(_, _, ex) =>
        ex.printStackTrace(args.args.out)
        ExitStatus.ParseError
      case Parsed.Success(tree) =>
        val doc = Doc.fromTree(tree)

        val fixed: String =
          if (args.rules.isSemantic) {
            val relpath = file.toRelative(args.args.sourceroot)
            val sdoc =
              SemanticDoc.fromPath(
                doc,
                relpath,
                args.args.classpath,
                args.symtab)
            args.rules.semanticPatch(sdoc)
          } else {
            args.rules.syntacticPatch(doc)
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
              Files.write(file.toNIO, fixed.getBytes(args.args.charset))
            }
            ExitStatus.Ok
          case WriteMode.AutoSuppressLinterErrors =>
            ???
        }

    }
  }

  def handleFile(args: ValidatedArgs, file: AbsolutePath): ExitStatus = {
    try unsafeHandleFile(args, file)
    catch {
      case NonFatal(e) =>
        e.printStackTrace(args.args.out)
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
    exit
  }

  def run(args: Seq[String], cwd: Path, out: PrintStream): ExitStatus =
    CliParser
      .parseArgs[Args](args.toList)
      .andThen(c => c.as[Args])
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
        out.println(err)
        ExitStatus.InvalidCommandLineOption
    }
}
