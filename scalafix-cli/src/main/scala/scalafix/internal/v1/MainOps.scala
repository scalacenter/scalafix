package scalafix.internal.v1

import java.io.PrintStream
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import metaconfig.Conf
import metaconfig.ConfEncoder
import metaconfig.annotation.Hidden
import metaconfig.annotation.Inline
import metaconfig.generic.Setting
import metaconfig.generic.Settings
import metaconfig.internal.Case
import org.typelevel.paiges.{Doc => D}
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ListBuffer
import scala.meta.io.AbsolutePath
import scala.meta.parsers.Parsed
import scala.util.control.NoStackTrace
import scala.util.control.NonFatal
import scalafix.Versions
import scalafix.cli.ExitStatus
import scalafix.diff.DiffUtils
import scalafix.internal.cli.WriteMode
import scalafix.lint.LintMessage
import scalafix.v1.Doc
import scalafix.v1.SemanticDoc

object MainOps {

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

  final class StaleSemanticDB(val path: AbsolutePath, val diff: String)
      extends Exception(s"Stale SemanticDB\n$diff")
      with NoStackTrace

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
      case Parsed.Error(pos, msg, _) =>
        args.config.reporter.error(msg, pos)
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
            val (fix, messages) =
              args.rules.semanticPatch(sdoc, args.args.autoSuppressLinterErrors)
            val isStaleSemanticDB = input.text != sdoc.sdoc.text
            val fixDoesNotMatchInput = input.text != fix
            if (isStaleSemanticDB && fixDoesNotMatchInput) {
              val diff = DiffUtils.unifiedDiff(
                file.toString() + "-ondisk",
                file.toString() + "-semanticdb",
                input.text.lines.toList,
                sdoc.sdoc.text.lines.toList,
                3
              )
              throw new StaleSemanticDB(file, diff)
            }
            (fix, messages)
          } else {
            args.rules.syntacticPatch(doc, args.args.autoSuppressLinterErrors)
          }

        if (!args.args.autoSuppressLinterErrors) {
          reportLintErrors(args, messages)
        }
        args.mode match {
          case WriteMode.Test =>
            if (fixed == input.text) {
              ExitStatus.Ok
            } else {
              val diff = DiffUtils.unifiedDiff(
                file.toString(),
                "<expected fix>",
                input.text.lines.toList,
                fixed.lines.toList,
                3
              )
              args.args.out.println(diff)
              ExitStatus.TestError
            }
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
        ExitStatus.MissingSemanticdbError
      case e: StaleSemanticDB =>
        if (args.args.noStaleSemanticdb) ExitStatus.Ok
        else {
          args.config.reporter.error(e.getMessage)
          ExitStatus.StaleSemanticdbError
        }
      case NonFatal(e) =>
        handleException(e, args.args.out)
        ExitStatus.UnexpectedError
    }
  }

  def run(args: ValidatedArgs): ExitStatus = {
    val files = this.files(args)
    var exit = ExitStatus.Ok
    files.foreach { file =>
      if (args.args.verbose) {
        args.config.reporter.info(s"Processing $file")
      }
      val next = handleFile(args, file)
      exit = ExitStatus.merge(exit, next)
    }
    adjustExitCode(args, exit, files)
  }

  def version =
    s"Scalafix ${Versions.version}"
  def usage: String =
    """|Usage: scalafix [options] [<path> ...]
       |""".stripMargin
  def description: D =
    D.paragraph(
      """|Scalafix is a refactoring and linting tool.
         |Scalafix supports both syntactic and semantic linter and rewrite rules.
         |Syntactic rules can run on source code without compilation.
         |Semantic rules can run on source code that has been compiled with the
         |SemanticDB compiler plugin.
         |""".stripMargin
    )

  /** Line wrap prose while keeping markdown code fences unchanged. */
  def markdownish(text: String): D = {
    val buf = ListBuffer.empty[String]
    val paragraphs = ListBuffer.empty[D]
    var insideCodeFence = false
    def flush(): Unit = {
      if (insideCodeFence) {
        paragraphs += D.intercalate(D.line, buf.map(D.text))
      } else {
        paragraphs += D.paragraph(buf.mkString("\n"))
      }
      buf.clear()
    }
    text.lines.foreach { line =>
      if (line.startsWith("```")) {
        flush()
        insideCodeFence = !insideCodeFence
      }
      buf += line
    }
    flush()
    D.intercalate(D.line, paragraphs)
  }

  def options(width: Int): String = {
    val sb = new StringBuilder()
    val settings = Settings[Args]
    val default = ConfEncoder[Args].writeObj(Args.default)
    def printOption(setting: Setting, value: Conf): Unit = {
      if (setting.annotations.exists(_.isInstanceOf[Hidden])) return
      setting.annotations.foreach {
        case section: Section =>
          sb.append("\n")
            .append(section.name)
            .append(":\n")
        case _ =>
      }
      val name = Case.camelToKebab(setting.name)
      sb.append("\n")
        .append("  --")
        .append(name)
      setting.extraNames.foreach { name =>
        if (name.length == 1) {
          sb.append(" | -")
            .append(Case.camelToKebab(name))
        }
      }
      if (!setting.isBoolean) {
        sb.append(" ")
          .append(setting.tpe)
          .append(" (default: ")
          .append(value.toString())
          .append(")")
      }
      sb.append("\n")
      setting.description.foreach { description =>
        sb.append("    ")
          .append(markdownish(description).nested(4).render(width))
          .append('\n')
      }
    }

    settings.settings.zip(default.values).foreach {
      case (setting, (_, value)) =>
        if (setting.annotations.exists(_.isInstanceOf[Inline])) {
          for {
            underlying <- setting.underlying.toList
            (field, (_, fieldDefault)) <- underlying.settings.zip(
              value.asInstanceOf[Conf.Obj].values)
          } {
            printOption(field, fieldDefault)
          }
        } else {
          printOption(setting, value)
        }
    }
    sb.toString()
  }

  def helpMessage(out: PrintStream, width: Int): Unit = {
    out.println(version)
    out.println(usage)
    out.println(description.render(width))
    out.println(options(width))
  }

}
