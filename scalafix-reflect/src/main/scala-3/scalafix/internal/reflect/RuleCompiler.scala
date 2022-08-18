package scalafix.internal.reflect

import dotty.tools.dotc.Compiler
import dotty.tools.dotc.Run
import dotty.tools.dotc.core.Contexts.FreshContext
import dotty.tools.dotc.util.SourceFile
import dotty.tools.dotc.reporting.StoreReporter
import dotty.tools.dotc.interactive.InteractiveDriver
import dotty.tools.io.AbstractFile as DottyAbstractFile
import dotty.tools.io.VirtualFile
import dotty.tools.io.VirtualDirectory as DottyVirtualDirectory

import scala.reflect.io.VirtualDirectory
import scala.reflect.io.PlainDirectory
import scala.reflect.io.AbstractFile
import scala.reflect.internal.util.AbstractFileClassLoader

import metaconfig.Configured
import metaconfig.Input
import metaconfig.ConfError
import metaconfig.Position

class RuleCompiler(
    classpath: String,
    target: AbstractFile = new VirtualDirectory("(memory)", None)
) {
  private val settings = "-unchecked" :: "-deprecation" :: "-classpath" :: classpath :: Nil
  private val driver = new InteractiveDriver(settings)
  private val reporter: StoreReporter = new StoreReporter()
  private var ctx: FreshContext = driver.currentCtx.fresh
  private val dottyTargetDirectory: DottyAbstractFile = {
    if (target.isVirtual)
      new DottyVirtualDirectory(target.name)
    else
      DottyAbstractFile.getDirectory(target.path)
  }
  ctx = ctx
    .setReporter(reporter)
    .setSetting(ctx.settings.outputDir, dottyTargetDirectory)
    .setSetting(ctx.settings.classpath, classpath)

  private val compiler: Compiler = new Compiler()
  private val classLoader: AbstractFileClassLoader =
    new AbstractFileClassLoader(target, this.getClass.getClassLoader)
  
  def compile(input: Input): Configured[ClassLoader] = {
    reporter.removeBufferedMessages(using ctx)
    val run: Run = compiler.newRun(using ctx)

    val dottyFile: DottyAbstractFile = input match {
      case Input.File(path, _) => DottyAbstractFile.getFile(input.path)
      case Input.VirtualFile(path, _) => VirtualFile(input.path, input.text.getBytes())
      case _ => throw RuntimeException("Invalid Input file")
    }

    run.compileSources(
      List(new SourceFile(dottyFile, input.chars))
    )

    val errors = reporter.allErrors.map(error => 
      ConfError
        .message(error.getMessage)
    )
    if (!errors.isEmpty) 
      errors :+ ConfError.message(
            "Error compiling rule(s) from source using Scala 3 compiler; " +
            "to use the Scala 2.x compiler instead, use the corresponding " +
            "scalafix-cli artifact or force scalafixScalaBinaryVersion " +
            "to 2.x in your build tool"
            )

    println(s"ipath ${input.path}")    
    println(s"try load ${classLoader.tryToLoadClass(input.path + "$")}")
    println(s"try load ${classLoader.tryToLoadClass(input.path)}")
    
    ConfError
      .apply(errors)
      .map(_.notOk)
      .getOrElse(Configured.Ok(classLoader))
  }
}