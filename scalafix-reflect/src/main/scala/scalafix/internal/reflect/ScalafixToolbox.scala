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

  private val ruleCache = new ConcurrentHashMap[RuleKey, CompilationMemo]()
  private val compilerCache =
    new ConcurrentHashMap[URLClassLoader, RuleCompiler]()

  def getRule(
      code: Input,
      toolClasspath: URLClassLoader
  ): Configured[CompiledRules] = {
    // Content is part of the key so that editing a file: rule between calls
    // triggers a recompilation instead of serving stale classes.
    val key = RuleKey(code, new String(code.chars), toolClasspath)
    val memo = ruleCache.computeIfAbsent(
      key,
      _ => new CompilationMemo(() => compile(code, toolClasspath))
    )
    val result = memo.value
    result match {
      case Configured.Ok(_) => ()
      // drop failures from the cache, so that transient ones can be retried
      case _ => ruleCache.remove(key, memo)
    }
    result
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

  // input carries the source identity (path), which the compilers consume
  private case class RuleKey(
      input: Input,
      content: String,
      toolClasspath: URLClassLoader
  )

  // lazy val, so that concurrent callers of the same key block on the memo
  // instead of compiling duplicates or locking the whole cache
  private class CompilationMemo(thunk: () => Configured[CompiledRules]) {
    lazy val value: Configured[CompiledRules] = thunk()
  }
}
