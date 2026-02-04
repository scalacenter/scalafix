package scalafix.internal.reflect

import java.io.File
import java.net.URLClassLoader
import java.nio.file.Paths

import scala.reflect.internal.util.AbstractFileClassLoader
import scala.reflect.internal.util.BatchSourceFile
import scala.reflect.io.AbstractFile
import scala.reflect.io.Directory
import scala.reflect.io.PlainDirectory
import scala.tools.nsc.Global
import scala.tools.nsc.io.VirtualDirectory
import scala.tools.nsc.reporters.StoreReporter

import metaconfig.ConfError
import metaconfig.Configured
import metaconfig.Input
import metaconfig.Position

/**
 * Compiles code with the Scala 2 compiler in the classpath.
 */
object CurrentClasspathCompiler {

  def compile(
      input: Input,
      toolClasspath: URLClassLoader,
      targetDirectory: Option[File]
  ): Configured[ClassLoader] = {
    val output: AbstractFile = targetDirectory match {
      case Some(file) => new PlainDirectory(new Directory(file))
      case None => new VirtualDirectory("(memory)", None)
    }
    val classpath = (toolClasspath.getURLs.map(url => Paths.get(url.toURI)) ++
      RuleCompilerClasspath.defaultClasspathPaths.map(_.toNIO))
      .mkString(File.pathSeparator)

    val settings = CompilerSetup.newSettings(classpath, output)
    val reporter = new StoreReporter
    val global = new Global(settings, reporter)

    reporter.reset()
    val run = new global.Run
    val label = input match {
      case Input.File(path, _) => path.toString
      case Input.VirtualFile(path, _) => path
      case _ => "(input)"
    }
    run.compileSources(
      List(new BatchSourceFile(label, new String(input.chars)))
    )

    val errors = reporter.infos.collect {
      case r: reporter.Info if r.severity == reporter.ERROR =>
        ConfError
          .message(r.msg)
          .atPos(
            if (r.pos.isDefined) Position.Range(input, r.pos.start, r.pos.end)
            else Position.None
          )
          .notOk
    }

    ConfError
      .fromResults(errors.toSeq)
      .map(_.notOk)
      .getOrElse {
        val classLoader: AbstractFileClassLoader =
          new AbstractFileClassLoader(output, toolClasspath)
        Configured.Ok(classLoader)
      }
  }
}
