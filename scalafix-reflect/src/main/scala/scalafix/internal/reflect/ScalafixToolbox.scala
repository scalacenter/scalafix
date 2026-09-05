package scalafix.internal.reflect

import java.net.URLClassLoader
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

import metaconfig.Configured
import metaconfig.Input
import scalafix.internal.config.MetaconfigOps._

/**
 * Compiles rules from source, memoizing results for its own lifetime (#782).
 * Owned by each `RuleDecoder.Settings`/`Args` by default, or shared across all
 * arguments of a `ScalafixImpl` so that clients control the cache lifecycle.
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
    val key = RuleKey(code, toolClasspath)
    // digest on the value side: editing a file: rule replaces its entry, so
    // stale classes are neither served nor retained
    val digest = sha256(code.chars)
    val memo = ruleCache.compute(
      key,
      (_, cached) =>
        if (cached != null && cached.contentDigest == digest) cached
        else new CompilationMemo(digest, () => compile(code, toolClasspath))
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
  private case class RuleKey(input: Input, toolClasspath: URLClassLoader)

  // lazy val, so that concurrent callers of the same key block on the memo
  // instead of compiling duplicates or locking the whole cache
  private class CompilationMemo(
      val contentDigest: Seq[Byte],
      thunk: () => Configured[CompiledRules]
  ) {
    lazy val value: Configured[CompiledRules] = thunk()
  }

  private def sha256(chars: Array[Char]): Seq[Byte] =
    MessageDigest
      .getInstance("SHA-256")
      .digest(new String(chars).getBytes("UTF-8"))
      .toSeq
}
