package scalafix.internal.reflect

import java.io.File
import java.net.URLClassLoader
import java.nio.file.Paths
import java.util.function
import scala.reflect.internal.util.AbstractFileClassLoader
import scala.reflect.internal.util.BatchSourceFile
import scala.tools.nsc.Global
import scala.tools.nsc.Settings
import scala.tools.nsc.io.AbstractFile
import scala.tools.nsc.io.VirtualDirectory
import scala.tools.nsc.reporters.StoreReporter
import scalafix.internal.config.MetaconfigPendingUpstream._
import scalafix.internal.config.LazySemanticdbIndex
import scalafix.internal.config.classloadRule
import scalafix.internal.util.ClassloadRule
import scalafix.rule.Rule
import metaconfig.ConfError
import metaconfig.Configured
import metaconfig.Input
import metaconfig.Position
import scala.meta.io.AbsolutePath

object ScalafixToolbox extends ScalafixToolbox
class ScalafixToolbox {
  private val ruleCache =
    new java.util.concurrent.ConcurrentHashMap[
      Input,
      Configured[CompiledRules]]()
  private val compilerCache =
    new java.util.concurrent.ConcurrentHashMap[String, RuleCompiler]()
  private val newCompiler: function.Function[String, RuleCompiler] =
    new function.Function[String, RuleCompiler] {
      override def apply(classpath: String) = new RuleCompiler(classpath)
    }

  case class CompiledRules(classloader: ClassLoader, fqns: Seq[String])

  def getRule(
      code: Input,
      toolClasspath: List[AbsolutePath]): Configured[CompiledRules] =
    ruleCache.getOrDefault(code, {
      val uncached = getRuleUncached(code, toolClasspath)
      uncached match {
        case toCache @ Configured.Ok(_) =>
          ruleCache.put(code, toCache)
        case _ =>
      }
      uncached
    })

  def getRuleUncached(
      code: Input,
      toolClasspath: List[AbsolutePath]): Configured[CompiledRules] =
    synchronized {
      val cp = RuleCompiler.defaultClasspathPaths ++ toolClasspath
      val compiler = compilerCache.computeIfAbsent(
        cp.mkString(File.pathSeparator),
        newCompiler
      )
      (
        compiler.compile(code) |@|
          RuleInstrumentation.getRuleFqn(code.toMeta)
      ).map(CompiledRules.tupled.apply)
    }
}

object RuleCompiler {

  def defaultClasspath: String = {
    defaultClasspathPaths.mkString(File.pathSeparator)
  }

  def defaultClasspathPaths: List[AbsolutePath] = {
    getClass.getClassLoader match {
      case u: URLClassLoader =>
        val paths = u.getURLs.iterator.map { u =>
          if (u.getProtocol.startsWith("bootstrap")) {
            import java.nio.file._
            val stream = u.openStream
            val tmp = Files.createTempFile("bootstrap-" + u.getPath, ".jar")
            Files.copy(stream, tmp, StandardCopyOption.REPLACE_EXISTING)
            AbsolutePath(tmp)
          } else {
            AbsolutePath(Paths.get(u.toURI))
          }
        }
        paths.toList
      case obtained =>
        throw new IllegalStateException(
          s"Classloader mismatch\n" +
            s"Expected: this.getClass.getClassloader.isInstanceOf[URLClassLoader]\n" +
            s"Obtained: $obtained")
    }
  }

}

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
      case Input.File(path, _) => path.toString()
      case Input.VirtualFile(label, _) => label
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
