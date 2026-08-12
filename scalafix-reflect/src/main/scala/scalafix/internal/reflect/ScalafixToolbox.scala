package scalafix.internal.reflect

import java.net.URLClassLoader

import metaconfig.Configured
import metaconfig.Input
import scalafix.internal.config.MetaconfigOps._

object ScalafixToolbox {

  case class CompiledRules(classloader: ClassLoader, fqns: Seq[String])

  def getRule(
      code: Input,
      toolClasspath: URLClassLoader
  ): Configured[CompiledRules] =
    (
      new RuleCompiler(toolClasspath).compile(code) |@|
        RuleInstrumentation.getRuleFqn(code.toMeta)
    ).map((CompiledRules.apply _).tupled)
}
