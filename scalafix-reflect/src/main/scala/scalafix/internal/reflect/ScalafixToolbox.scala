package scalafix.internal.reflect

import java.net.URLClassLoader
import java.util.concurrent.ConcurrentHashMap

import metaconfig.Configured
import metaconfig.Input
import scalafix.internal.config.MetaconfigOps._

/**
 * Compiles rules from source and memoizes results for its own lifetime.
 *
 * Each Scalafix API instance owns a toolbox, so that API clients control when
 * caching starts and stops by creating or dropping instances (issue #782).
 */
class ScalafixToolbox {
  import ScalafixToolbox._

  private val ruleCache =
    new ConcurrentHashMap[RuleKey, Configured[CompiledRules]]()
  private val compilerCache =
    new ConcurrentHashMap[URLClassLoader, RuleCompiler]()

  def getRule(
      code: Input,
      toolClasspath: URLClassLoader
  ): Configured[CompiledRules] = {
    // Keyed on content rather than Input, so that editing a file: rule between
    // calls triggers a recompilation instead of serving stale classes.
    val key = RuleKey(new String(code.chars), toolClasspath)
    ruleCache.get(key) match {
      case null =>
        compile(code, toolClasspath) match {
          case ok @ Configured.Ok(_) =>
            Option(ruleCache.putIfAbsent(key, ok)).getOrElse(ok)
          case notOk => notOk
        }
      case cached => cached
    }
  }

  private def compile(
      code: Input,
      toolClasspath: URLClassLoader
  ): Configured[CompiledRules] = {
    val compiler =
      compilerCache.computeIfAbsent(toolClasspath, new RuleCompiler(_))
    (
      compiler.synchronized(compiler.compile(code)) |@|
        RuleInstrumentation.getRuleFqn(code.toMeta)
    ).map((CompiledRules.apply _).tupled)
  }
}

object ScalafixToolbox {
  case class CompiledRules(classloader: ClassLoader, fqns: Seq[String])

  private case class RuleKey(code: String, toolClasspath: URLClassLoader)
}
