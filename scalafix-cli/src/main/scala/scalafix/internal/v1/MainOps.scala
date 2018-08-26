package scalafix.internal.v1

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.CharBuffer
import java.nio.charset.StandardCharsets
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import metaconfig.Conf
import metaconfig.ConfEncoder
import metaconfig.Configured
import metaconfig.annotation.Hidden
import metaconfig.annotation.Inline
import metaconfig.generic.Setting
import metaconfig.generic.Settings
import metaconfig.internal.Case
import org.typelevel.paiges.{Doc => D}
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ListBuffer
import scala.meta.Tree
import scala.meta.inputs.Input
import scala.meta.internal.semanticdb.TextDocument
import scala.meta.internal.tokenizers.PlatformTokenizerCache
import scala.meta.io.AbsolutePath
import scala.meta.parsers.ParseException
import scala.util.control.NoStackTrace
import scala.util.control.NonFatal
import scalafix.Versions
import scalafix.cli.ExitStatus
import scalafix.internal.config.PrintStreamReporter
import scalafix.internal.diff.DiffUtils
import scalafix.v1.Doc
import scalafix.v1.SemanticDoc

object MainOps {

  def run(args: Array[String], base: Args): ExitStatus = {
    val expanded = ArgExpansion.expand(args, base.cwd)
    val out = base.out
    Conf
      .parseCliArgs[Args](expanded)
      .andThen(c => c.as[Args](Args.decoder(base)))
      .andThen(_.validate) match {
      case Configured.Ok(validated) =>
        if (validated.args.help) {
          MainOps.helpMessage(out, 80)
          ExitStatus.Ok
        } else if (validated.args.version) {
          out.println(Versions.version)
          ExitStatus.Ok
        } else if (validated.args.bash) {
          out.println(CompletionsOps.bashCompletions)
          ExitStatus.Ok
        } else if (validated.args.zsh) {
          out.println(CompletionsOps.zshCompletions)
          ExitStatus.Ok
        } else if (validated.rules.isEmpty) {
          out.println("Missing --rules")
          ExitStatus.CommandLineError
        } else {
          MainOps.run(validated)
        }
      case Configured.NotOk(err) =>
        PrintStreamReporter(out = out).error(err.toString())
        ExitStatus.CommandLineError
    }
  }

  def files(args: ValidatedArgs): Seq[AbsolutePath] = args.args.ls match {
    case Ls.Find =>
      val buf = ArrayBuffer.empty[AbsolutePath]
      val visitor = new SimpleFileVisitor[Path] {
        override def visitFile(
            file: Path,
            attrs: BasicFileAttributes
        ): FileVisitResult = {
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
    if (args.callback.hasLintErrors) {
      ExitStatus.merge(ExitStatus.LinterError, code)
    } else if (args.callback.hasErrors && code.isOk) {
      ExitStatus.merge(ExitStatus.UnexpectedError, code)
    } else if (files.isEmpty) {
      args.config.reporter.error("No files to fix")
      ExitStatus.merge(ExitStatus.NoFilesError, code)
    } else {
      code
    }
  }

  def assertFreshSemanticDB(
      input: Input,
      file: AbsolutePath,
      fix: String,
      doc: TextDocument
  ): Unit = {
    if (input.text == fix) {
      // Fix is a no-op, ignore. This frequently happens in cross-built modules
      // where the JVM project fixes sources and the Scala.js project becomes stale.
      ()
    } else if (doc.md5.isEmpty) {
      throw new IllegalArgumentException("-P:semanticdb:md5:on is required.")
    } else {
      val inputMD5 = FingerprintOps.md5(
        StandardCharsets.UTF_8.encode(CharBuffer.wrap(input.chars))
      )
      if (inputMD5 == doc.md5) {
        () // OK!
      } else {
        val diff = if (doc.text.isEmpty) {
          DiffUtils.unifiedDiff(
            input.syntax + "-ondisk-md5-fingerprint",
            input.syntax + "-semanticdb-md5-fingerprint",
            inputMD5 :: Nil,
            doc.md5 :: Nil,
            1
          )
        } else {
          DiffUtils.unifiedDiff(
            input.syntax + "-ondisk",
            input.syntax + "-semanticdb",
            input.text.lines.toList,
            doc.text.lines.toList,
            3
          )
        }
        throw new StaleSemanticDB(file, diff)
      }
    }
  }

  def unsafeHandleFile(args: ValidatedArgs, file: AbsolutePath): ExitStatus = {
    val input = args.input(file)
    val tree = LazyValue.later { () =>
      args.parse(input).get: Tree
    }
    val doc = Doc(input, tree, args.diffDisable, args.config)
    val (fixed, messages) =
      if (args.rules.isSemantic) {
        val relpath = file.toRelative(args.sourceroot)
        val sdoc = SemanticDoc.fromPath(
          doc,
          relpath,
          args.classLoader,
          args.symtab
        )
        val (fix, messages) =
          args.rules.semanticPatch(sdoc, args.args.autoSuppressLinterErrors)
        assertFreshSemanticDB(input, file, fix, sdoc.internal.textDocument)
        (fix, messages)
      } else {
        args.rules.syntacticPatch(doc, args.args.autoSuppressLinterErrors)
      }

    if (!args.args.autoSuppressLinterErrors) {
      messages.foreach { diag =>
        args.config.reporter.lint(diag)
      }
    }

    if (args.args.test) {
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
    } else if (args.args.stdout) {
      args.args.out.println(fixed)
      ExitStatus.Ok
    } else {
      if (fixed != input.text) {
        val toFix = args.pathReplace(file).toNIO
        Files.createDirectories(toFix.getParent)
        Files.write(toFix, fixed.getBytes(args.args.charset))
      }
      ExitStatus.Ok
    }
  }

  def handleFile(args: ValidatedArgs, file: AbsolutePath): ExitStatus = {
    try {
      PlatformTokenizerCache.megaCache.clear()
      unsafeHandleFile(args, file)
    } catch {
      case e: ParseException =>
        args.config.reporter.error(e.shortMessage, e.pos)
        ExitStatus.ParseError
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
    var i = 0
    val N = files.length
    val width = N.toString.length
    var exit = ExitStatus.Ok
    files.foreach { file =>
      if (args.args.verbose) {
        val message = s"Processing (%${width}s/%s) %s".format(i, N, file)
        args.config.reporter.info(message)
        i += 1
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
            (field, (_, fieldDefault)) <- underlying.settings
              .zip(value.asInstanceOf[Conf.Obj].values)
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
  def helpMessage(width: Int): String = {
    val baos = new ByteArrayOutputStream()
    helpMessage(new PrintStream(baos), width)
    baos.toString(StandardCharsets.UTF_8.name())
  }

}
