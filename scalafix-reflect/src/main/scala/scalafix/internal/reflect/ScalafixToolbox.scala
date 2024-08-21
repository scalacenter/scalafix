package scalafix.internal.reflect

import java.net.URLClassLoader
import java.util.function

import metaconfig.Configured
import metaconfig.Input
import scalafix.internal.config.MetaconfigOps._

object ScalafixToolbox extends ScalafixToolbox
class ScalafixToolbox {

  private val ruleCache =
    new java.util.concurrent.ConcurrentHashMap[Input, Configured[
      CompiledRules
    ]]()
  private val compilerCache =
    new java.util.concurrent.ConcurrentHashMap[URLClassLoader, RuleCompiler]()
  private val newCompiler: function.Function[URLClassLoader, RuleCompiler] =
    new function.Function[URLClassLoader, RuleCompiler] {
      override def apply(toolClasspath: URLClassLoader) =
        new RuleCompiler(toolClasspath)
    }

  case class CompiledRules(classloader: ClassLoader, fqns: Seq[String])

  def getRule(
      code: Input,
      toolClasspath: URLClassLoader
  ): Configured[CompiledRules] =
    ruleCache.getOrDefault(
      code, {
        val uncached = getRuleUncached(code, toolClasspath)
        uncached match {
          case toCache @ Configured.Ok(_) =>
            ruleCache.put(code, toCache)
          case _ =>
        }
        uncached
      }
    )

  def getRuleUncached(
      code: Input,
      toolClasspath: URLClassLoader
  ): Configured[CompiledRules] =
    synchronized {
      val compiler =
        compilerCache.computeIfAbsent(toolClasspath, newCompiler)
      (
        compiler.compile(code) |@|
          RuleInstrumentation.getRuleFqn(code.toMeta)
      ).map((CompiledRules.apply _).tupled)
    }
}
