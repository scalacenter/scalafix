package scalafix.internal.reflect

import java.io.File
import java.net.URLClassLoader
import java.util.function
import metaconfig.Configured
import metaconfig.Input
import scalafix.internal.config.MetaconfigPendingUpstream._

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
      override def apply(classpath: String) =
        new RuleCompiler(
          classpath + File.pathSeparator + RuleCompiler.defaultClasspath)
    }

  case class CompiledRules(classloader: ClassLoader, fqns: Seq[String])

  def getRule(
      code: Input,
      toolClasspath: URLClassLoader): Configured[CompiledRules] =
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
      toolClasspath: URLClassLoader): Configured[CompiledRules] =
    synchronized {
      val classpath =
        toolClasspath.getURLs.map(_.getPath).mkString(File.pathSeparator)
      val compiler = compilerCache.computeIfAbsent(classpath, newCompiler)
      (
        compiler.compile(code) |@|
          RuleInstrumentation.getRuleFqn(code.toMeta)
      ).map(CompiledRules.tupled.apply)
    }
}
