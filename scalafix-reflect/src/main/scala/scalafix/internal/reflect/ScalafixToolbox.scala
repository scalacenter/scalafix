package scalafix.internal.reflect

import java.io.File
import java.net.URLClassLoader
import java.net.URLDecoder
import scala.meta.inputs.Input
import scala.reflect.internal.util.AbstractFileClassLoader
import scala.reflect.internal.util.BatchSourceFile
import scala.tools.nsc.Global
import scala.tools.nsc.Settings
import scala.tools.nsc.io.VirtualDirectory
import scala.tools.nsc.reporters.StoreReporter
import scala.{meta => m}
import scalafix.internal.config.LazySemanticdbIndex
import scalafix.internal.config.classloadRule
import scalafix.internal.util.ClassloadRule
import scalafix.rule.Rule
import metaconfig.ConfError
import metaconfig.Configured

object ScalafixToolbox extends ScalafixToolbox
class ScalafixToolbox {
  private val ruleCache =
    new java.util.concurrent.ConcurrentHashMap[Input, Configured[Rule]]()
  private val compiler = new Compiler()

  def getRule(code: Input, index: LazySemanticdbIndex): Configured[Rule] =
    ruleCache.getOrDefault(code, {
      val uncached = getRuleUncached(code, index)
      uncached match {
        case toCache @ Configured.Ok(_) =>
          ruleCache.put(code, toCache)
        case _ =>
      }
      uncached
    })

  def getRuleUncached(code: Input, index: LazySemanticdbIndex): Configured[Rule] =
    synchronized {
      (
        compiler.compile(code) |@|
          RuleInstrumentation.getRuleFqn(code)
      ).andThen {
        case (classloader, names) =>
          names.foldLeft(Configured.ok(Rule.empty)) {
            case (rule, fqn) =>
              val args = classloadRule(index)
              rule
                .product(ClassloadRule(fqn, args, classloader))
                .map { case (a, b) => a.merge(b) }
          }
      }
    }
}

class Compiler() {
  val target = new VirtualDirectory("(memory)", None)
  private val settings = new Settings()
  settings.deprecation.value = true // enable detailed deprecation warnings
  settings.unchecked.value = true // enable detailed unchecked warnings
  settings.outputDirs.setSingleOutput(target)
  getClass.getClassLoader match {
    case u: URLClassLoader =>
      settings.classpath.value = u.getURLs
        .map(x => URLDecoder.decode(x.getPath, "UTF-8"))
        .mkString(File.pathSeparator)
    case _ => ""
  }
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
          .msg(msg)
          .atPos(
            if (pos.isDefined) m.Position.Range(input, pos.start, pos.end)
            else m.Position.None
          )
          .notOk
    }
    ConfError
      .fromResults(errors.toSeq)
      .map(_.notOk)
      .getOrElse(Configured.Ok(classLoader))
  }
}
