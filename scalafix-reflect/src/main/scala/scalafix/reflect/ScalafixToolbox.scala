package scalafix.reflect

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
import scalafix.rewrite.Rewrite
import scalafix.util.ClassloadRewrite

import java.io.File
import java.net.URLClassLoader

import metaconfig.ConfError
import metaconfig.Configured

object ScalafixToolbox {
  import scala.reflect.runtime.universe._
  import scala.tools.reflect.ToolBox
  private val tb = runtimeMirror(getClass.getClassLoader).mkToolBox()
  private val rewriteCache
    : mutable.WeakHashMap[Input, Configured.Ok[Rewrite]] =
    mutable.WeakHashMap.empty
  private val compiler = new Compiler()
  lazy val emptyRewrite: Configured[Rewrite] =
    Configured.Ok(Rewrite.empty)

  def getRewrite(code: Input, mirror: Option[m.Mirror]): Configured[Rewrite] =
    rewriteCache.getOrElse(code, {
      val uncached = getRewriteUncached(code, mirror)
      uncached match {
        case toCache @ Configured.Ok(_) => rewriteCache(code) = toCache
        case _ =>
      }
      uncached
    })

  def getRewriteUncached(code: Input,
                         mirror: Option[m.Mirror]): Configured[Rewrite] =
    for {
      classloader <- compiler.compile(code)
      names <- RewriteInstrumentation.getRewriteFqn(code)
      rewrite <- names.foldLeft(emptyRewrite) {
        case (rewrite, fqn) =>
          rewrite
            .product(ClassloadRewrite(fqn, mirror.toList, classloader))
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
      case Input.File(path, _) => path.absolute
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
