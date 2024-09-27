package scalafix.internal.rule

import scala.meta.*

import dotty.tools.pc.ScalaPresentationCompiler
import metaconfig.Configured
import scalafix.internal.pc.PcExplicitResultTypes
import scalafix.patch.Patch
import scalafix.v1.*

final class ExplicitResultTypes(
    val config: ExplicitResultTypesConfig,
    fallback: Option[PcExplicitResultTypes]
) extends ExplicitResultTypesBase[Scala3Printer] {

  def this() = this(ExplicitResultTypesConfig.default, None)

  override def afterComplete(): Unit = {
    shutdownCompiler()
  }

  private def shutdownCompiler(): Unit = {
    fallback.foreach(_.shutdownCompiler())
  }

  override def withConfiguration(config: Configuration): Configured[Rule] = {
    config.conf // Support deprecated explicitReturnTypes config
      .getOrElse("explicitReturnTypes", "ExplicitResultTypes")(
        ExplicitResultTypesConfig.default
      )
      .map(c =>
        new ExplicitResultTypes(
          c,
          Option {
            if (
              stripPatchVersion(config.scalaVersion) ==
                stripPatchVersion(compilerScalaVersion)
            )
              PcExplicitResultTypes
                .static(config, new ScalaPresentationCompiler())
            else
              PcExplicitResultTypes.dynamic(config)
          }
        )
      )
  }

  override def fix(implicit ctx: SemanticDocument): Patch =
    try {
      implicit val printer = new Scala3Printer(fallback)
      unsafeFix()
    } catch {
      case _: Throwable if !config.fatalWarnings =>
        Patch.empty
    }

}

class Scala3Printer(
    fallback: Option[PcExplicitResultTypes]
) extends Printer {

  def defnType(
      defn: Defn,
      replace: Token,
      space: String
  )(implicit
      ctx: SemanticDocument
  ): Option[Patch] = {
    fallback.flatMap(_.defnType(replace))
  }
}
