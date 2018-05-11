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
import scalafix.internal.cli.WriteMode
import scalafix.internal.util.SymbolTable

object Main {

  def files(args: Args): Seq[AbsolutePath] = args.ls match {
    case Ls.Find =>
      val buf = ArrayBuffer.empty[AbsolutePath]
      Files.walkFileTree(
        args.cwd.toNIO,
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

  def unsafeHandleFile(
      args: Args,
      symtab: SymbolTable,
      file: AbsolutePath): ExitStatus = {
    val input = args.input(file)
    args.parse(input) match {
      case Parsed.Error(_, _, ex) =>
        ex.printStackTrace(args.out)
        ExitStatus.ParseError
      case Parsed.Success(tree) =>
        val doc = Doc.fromTree(tree)

        val fixed: String =
          if (args.rules.isSemantic) {
            val relpath = file.toRelative(args.sourceroot)
            val sdoc =
              SemanticDoc.fromPath(doc, relpath, args.classpath, symtab)
            args.rules.semanticPatch(sdoc)
          } else {
            args.rules.syntacticPatch(doc)
          }

        args.mode match {
          case WriteMode.Test =>
            if (fixed == input.text) ExitStatus.Ok
            else ExitStatus.TestFailed
          case WriteMode.Stdout =>
            args.out.println(fixed)
            ExitStatus.Ok
          case WriteMode.WriteFile =>
            if (fixed != input.text) {
              Files.write(file.toNIO, fixed.getBytes(args.charset))
            }
            ExitStatus.Ok
          case WriteMode.AutoSuppressLinterErrors =>
            ???
        }

    }
  }

  def handleFile(
      args: Args,
      symtab: SymbolTable,
      file: AbsolutePath): ExitStatus = {
    try unsafeHandleFile(args, symtab, file)
    catch {
      case NonFatal(e) =>
        e.printStackTrace(args.out)
        ExitStatus.UnexpectedError
    }
  }

  def run(args: Args): ExitStatus = {
    val files = this.files(args)
    val st = ClasspathOps.newSymbolTable(
      classpath = args.classpath,
      cacheDirectory = args.metacpCacheDir.headOption,
      parallel = args.metacpParallel,
      out = args.out
    )
    st match {
      case Some(symtab) =>
        var exit = ExitStatus.Ok
        files.foreach { file =>
          val next = handleFile(args, symtab, file)
          exit = ExitStatus.merge(exit, next)
        }
        exit
      case _ =>
        args.out.println("Unable to load symtab")
        ExitStatus.UnexpectedError
    }
  }

  def run(args: Array[String], cwd: Path, out: PrintStream): ExitStatus =
    Conf.parseCliArgs[Args](args.toList).andThen(_.as[Args]) match {
      case Configured.Ok(a) =>
        if (a.rules.isEmpty) {
          out.println("Missing --rules")
          ExitStatus.InvalidCommandLineOption
        } else {
          val adjusted = a.copy(
            out = out,
            cwd = AbsolutePath(cwd)
          )
          run(adjusted)
        }
      case Configured.NotOk(err) =>
        out.println(err)
        ExitStatus.InvalidCommandLineOption
    }
}
