package scalafix.internal.reflect

import java.io.File
import java.net.URLClassLoader
import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.meta.inputs.Input
import scala.reflect.internal.util.AbstractFileClassLoader
import scala.reflect.internal.util.BatchSourceFile
import scala.tools.nsc.Global
import scala.tools.nsc.Settings
import scala.tools.nsc.io.VirtualDirectory
import scala.tools.nsc.reporters.StoreReporter
import scala.{meta => m}
import scalafix.config.LazyMirror
import scalafix.config.classloadRewrite
import scalafix.internal.util.ClassloadRewrite
import scalafix.rewrite.Rewrite
import metaconfig.ConfError
import metaconfig.Configured

object ScalafixToolbox {
  private val rewriteCache
    : mutable.WeakHashMap[Input, Configured.Ok[Rewrite]] =
    mutable.WeakHashMap.empty
  private val compiler = new Compiler()

  def getRewrite(code: Input, mirror: LazyMirror): Configured[Rewrite] =
    rewriteCache.getOrElse(code, {
      val uncached = getRewriteUncached(code, mirror)
      uncached match {
        case toCache @ Configured.Ok(_) => rewriteCache(code) = toCache
        case _ =>
      }
      uncached
    })

  def getRewriteUncached(code: Input,
                         mirror: LazyMirror): Configured[Rewrite] =
    for {
      classloader <- compiler.compile(code)
      names <- RewriteInstrumentation.getRewriteFqn(code)
      rewrite <- names.foldLeft(Rewrite.emptyConfigured) {
        case (rewrite, fqn) =>
          rewrite
            .product(
              ClassloadRewrite(fqn, classloadRewrite(mirror), classloader))
            .map { case (a, b) => a.andThen(b) }
      }
    } yield rewrite
}

class Compiler() {
  val target = new VirtualDirectory("(memory)", None)
  private val settings = new Settings()
  settings.deprecation.value = true // enable detailed deprecation warnings
  settings.unchecked.value = true // enable detailed unchecked warnings
  settings.outputDirs.setSingleOutput(target)
  getClass.getClassLoader match {
    case u: URLClassLoader =>
      settings.classpath.value =
        u.getURLs.map(_.getPath).mkString(File.pathSeparator)
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
      case Input.LabeledString(label, _) => label
      case _ => "(input)"
    }
    run.compileSources(
      List(new BatchSourceFile(label, new String(input.chars))))
    val errors = reporter.infos.collect {
      case reporter.Info(pos, msg, reporter.ERROR) =>
        ConfError
          .msg(msg)
          .atPos(m.Position.Range(input, pos.start, pos.end))
          .notOk
    }
    ConfError
      .fromResults(errors.toSeq)
      .map(_.notOk)
      .getOrElse(Configured.Ok(classLoader))
  }
}
