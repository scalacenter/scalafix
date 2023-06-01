package scalafix.internal.reflect

import dotty.tools.dotc.Compiler
import dotty.tools.dotc.Run
import dotty.tools.dotc.core.Contexts.FreshContext
import dotty.tools.dotc.util.SourceFile
import dotty.tools.dotc.reporting.StoreReporter
import dotty.tools.dotc.interactive.InteractiveDriver
import dotty.tools.io.AbstractFile
import dotty.tools.io.Directory
import dotty.tools.io.PlainDirectory
import dotty.tools.io.VirtualFile
import dotty.tools.io.VirtualDirectory
import dotty.tools.repl.AbstractFileClassLoader

import metaconfig.Configured
import metaconfig.Input
import metaconfig.ConfError
import metaconfig.Position

import java.io.File
class RuleCompiler(
    classpath: String,
    targetDirectory: Option[File] = None
) {
  private val output = targetDirectory match {
    case Some(file) => new PlainDirectory(new Directory(file.toPath))
    case None => new VirtualDirectory("(memory)")
  }
  private val settings =
    "-unchecked" :: "-deprecation" :: "-classpath" :: classpath :: Nil
  private val driver = new InteractiveDriver(settings)
  private val reporter: StoreReporter = new StoreReporter()
  private var ctx: FreshContext = driver.currentCtx.fresh
  ctx = ctx
    .setReporter(reporter)
    .setSetting(ctx.settings.outputDir, output)
    .setSetting(ctx.settings.classpath, classpath)

  private val compiler: Compiler = new Compiler()

  def compile(input: Input): Configured[ClassLoader] = {
    reporter.removeBufferedMessages(using ctx)
    val run: Run = compiler.newRun(using ctx)

    val file: AbstractFile = input match {
      case Input.File(path, _) => AbstractFile.getFile(input.path)
      case Input.VirtualFile(path, _) =>
        VirtualFile(input.path, input.text.getBytes())
      case _ => throw RuntimeException("Invalid Input file")
    }

    run.compileSources(
      List(new SourceFile(file, input.chars))
    )

    if (reporter.allErrors.isEmpty) {
      val classLoader: AbstractFileClassLoader =
        new AbstractFileClassLoader(output, this.getClass.getClassLoader)
      Configured.Ok(classLoader)
    } else {
      val lastError =
        "Error compiling rule(s) from source using Scala 3 compiler; " +
          "to use the Scala 2.x compiler instead, use the corresponding " +
          "scalafix-cli artifact or force scalafixScalaBinaryVersion " +
          "to 2.x in your build tool"
      val errors = (reporter.allErrors.map(_.message) :+ lastError)
      ConfError.apply(errors.map(ConfError.message)).map(_.notOk).get
    }

  }
}
