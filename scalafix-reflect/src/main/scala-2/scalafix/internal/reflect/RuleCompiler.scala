package scalafix.internal.reflect
import java.io.File
import java.net.URLClassLoader

import metaconfig.Configured
import metaconfig.Input

class RuleCompiler(
    toolClasspath: URLClassLoader,
    targetDirectory: Option[File] = None
) {

  def compile(input: Input): Configured[ClassLoader] = {
    // When running from Scala 3.8+ runtime, we must use isolated compilation to
    // avoid binary incompatibility between Scala 3 stdlib and Scala 2.13 compiler.
    // See https://github.com/scala/scala3/issues/25145 for more details.
    if (scala.util.Properties.versionNumberString.startsWith("3.")) {
      IsolatedScala213Compiler.compile(input, toolClasspath, targetDirectory)
    } else {
      // Fast path for Scala 2 runtimes - use in-classpath compiler
      CurrentClasspathCompiler.compile(input, toolClasspath, targetDirectory)
    }
  }
}
