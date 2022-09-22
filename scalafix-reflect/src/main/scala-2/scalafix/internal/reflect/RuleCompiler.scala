package scalafix.internal.reflect
import java.io.File

import scala.reflect.internal.util.AbstractFileClassLoader
import scala.reflect.internal.util.BatchSourceFile
import scala.reflect.io.Directory
import scala.reflect.io.PlainDirectory
import scala.tools.nsc.Global
import scala.tools.nsc.Settings
import scala.tools.nsc.io.VirtualDirectory
import scala.tools.nsc.reporters.StoreReporter

import metaconfig.ConfError
import metaconfig.Configured
import metaconfig.Input
import metaconfig.Position

class RuleCompiler(
    classpath: String,
    targetDirectory: Option[File] = None
) {
  private val output = targetDirectory match {
    case Some(file) => new PlainDirectory(new Directory(file))
    case None => new VirtualDirectory("(memory)", None)
  }
  private val settings = new Settings()
  settings.deprecation.value = true // enable detailed deprecation warnings
  settings.unchecked.value = true // enable detailed unchecked warnings
  settings.outputDirs.setSingleOutput(output)
  settings.classpath.value = classpath
  lazy val reporter = new StoreReporter
  private val global = new Global(settings, reporter)
  private val classLoader =
    new AbstractFileClassLoader(output, this.getClass.getClassLoader)

  def compile(input: Input): Configured[ClassLoader] = {
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
      case reporter.Info(pos, msg, reporter.ERROR) =>
        ConfError
          .message(msg)
          .atPos(
            if (pos.isDefined) Position.Range(input, pos.start, pos.end)
            else Position.None
          )
          .notOk
    }
    ConfError
      .fromResults(errors.toSeq)
      .map(_.notOk)
      .getOrElse(Configured.Ok(classLoader))
  }
}
