package scalafix.internal.reflect
import java.io.File
import java.nio.file.Paths
import metaconfig.ConfError
import metaconfig.Configured
import metaconfig.Input
import metaconfig.Position
import scala.meta.io.AbsolutePath
import scala.reflect.internal.util.AbstractFileClassLoader
import scala.reflect.internal.util.BatchSourceFile
import scala.tools.nsc.Global
import scala.tools.nsc.Settings
import scala.tools.nsc.io.AbstractFile
import scala.tools.nsc.io.VirtualDirectory
import scala.tools.nsc.reporters.StoreReporter

class RuleCompiler(
    classpath: String,
    target: AbstractFile = new VirtualDirectory("(memory)", None)) {
  private val settings = new Settings()
  settings.deprecation.value = true // enable detailed deprecation warnings
  settings.unchecked.value = true // enable detailed unchecked warnings
  settings.outputDirs.setSingleOutput(target)
  settings.classpath.value = classpath
  lazy val reporter = new StoreReporter
  private val global = new Global(settings, reporter)
  private val classLoader =
    new AbstractFileClassLoader(target, this.getClass.getClassLoader)

  def compile(input: Input): Configured[ClassLoader] = {
    reporter.reset()
    val run = new global.Run
    val label = input match {
      case Input.File(path, _) => path.toString
      case Input.VirtualFile(path, _) => path
      case _ => "(input)"
    }
    run.compileSources(
      List(new BatchSourceFile(label, new String(input.chars))))
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
object RuleCompiler {

  def defaultClasspath: String = {
    defaultClasspathPaths.mkString(File.pathSeparator)
  }

  def defaultClasspathPaths: List[AbsolutePath] = {
    val classLoader = ClasspathOps.thisClassLoader
    val paths = classLoader.getURLs.iterator.map { u =>
      if (u.getProtocol.startsWith("bootstrap")) {
        import java.nio.file._
        val stream = u.openStream
        val tmp = Files.createTempFile("bootstrap-" + u.getPath, ".jar")
        try {
          Files.copy(stream, tmp, StandardCopyOption.REPLACE_EXISTING)
        } finally {
          stream.close()
        }
        AbsolutePath(tmp)
      } else {
        AbsolutePath(Paths.get(u.toURI))
      }
    }
    paths.toList
  }

}
