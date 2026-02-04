package scalafix.internal.reflect.bridge

import java.io.File

import scala.jdk.CollectionConverters._
import scala.reflect.internal.util.BatchSourceFile
import scala.reflect.io.Directory
import scala.reflect.io.PlainDirectory
import scala.tools.nsc.Global
import scala.tools.nsc.reporters.StoreReporter

import scalafix.internal.reflect.CompilerSetup

/**
 * This class is meant to be loaded reflectively in an isolated environment to
 * avoid classpath contamination. Uses only simple Java types in the public API
 * to safely cross classloader boundaries.
 */
class Scala2CompilerBridge {

  // Changes in this method signature must be mirrored in IsolatedScala213Compiler which uses reflection
  def compile(
      source: String,
      filename: String,
      classpath: String,
      outputDir: String
  ): CompilationResult = {
    val outputFile = new File(outputDir)
    outputFile.mkdirs()

    val output = new PlainDirectory(new Directory(outputFile))

    val settings = CompilerSetup.newSettings(classpath, output)
    val reporter = new StoreReporter()
    val global = new Global(settings, reporter)

    try {
      val run = new global.Run()
      val sourceFile = new BatchSourceFile(filename, source)
      run.compileSources(List(sourceFile))

      val messages: java.util.List[CompilerMessage] = reporter.infos
        .map { info =>
          new CompilerMessage(
            info.severity.toString,
            if (info.pos.isDefined) info.pos.start else 0,
            if (info.pos.isDefined) info.pos.end else 0,
            info.msg
          )
        }
        .toList
        .asJava

      val success = !reporter.hasErrors

      new CompilationResult(success, messages)
    } catch {
      case ex: Throwable =>
        val message = new CompilerMessage(
          "ERROR",
          0,
          0,
          s"Compilation failed with exception: ${ex.getClass.getName}: ${ex.getMessage}"
        )
        new CompilationResult(
          false,
          java.util.Collections.singletonList(message)
        )
    }
  }
}
