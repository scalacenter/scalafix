package scalafix.internal.rule

import scala.meta.*

import dotty.tools.pc.ScalaPresentationCompiler
import metaconfig.Configured
import scalafix.internal.pc.PresentationCompilerTypeInferrer
import scalafix.patch.Patch
import scalafix.v1.*

final class ExplicitResultTypes(
    val config: ExplicitResultTypesConfig,
    pcTypeInferrer: Option[PresentationCompilerTypeInferrer]
) extends ExplicitResultTypesBase[Scala3Printer] {

  def this() = this(ExplicitResultTypesConfig.default, None)

  override def afterComplete(): Unit = {
    shutdownCompiler()
  }

  private def shutdownCompiler(): Unit = {
    pcTypeInferrer.foreach(_.shutdownCompiler())
  }

  override def withConfiguration(config: Configuration): Configured[Rule] = {
    val majorMinorScalaVersion =
      stripPatchVersion(config.scalaVersion)

    if (config.scalaVersion.startsWith("2.")) {
      Configured.error(
        s"The ExplicitResultTypes rule needs to run with the same Scala binary version as the one used to compile target sources ($majorMinorScalaVersion). " +
          s"To fix this problem, either remove ExplicitResultTypes from .scalafix.conf or make sure Scalafix is loaded with $majorMinorScalaVersion."
      )
    } else if (
      Seq("3.0", "3.1", "3.2").exists(v => config.scalaVersion.startsWith(v))
    ) {
      Configured.error(
        s"The ExplicitResultTypes rule requires Scala 3 target sources to be compiled with Scala 3.3.0 or greater, but they were compiled with ${config.scalaVersion}. " +
          "To fix this problem, either remove ExplicitResultTypes from .scalafix.conf or upgrade the compiler in your build."
      )
    } else {
      config.conf.getOrElse("ExplicitResultTypes")(this.config).andThen {
        conf =>
          val majorMinorCompilerScalaVersion =
            stripPatchVersion(compilerScalaVersion)

          val matchingMinors =
            majorMinorScalaVersion == majorMinorCompilerScalaVersion

          if (
            !matchingMinors && !conf.fetchScala3CompilerArtifactsOnVersionMismatch
          ) {
            Configured.error(
              s"The ExplicitResultTypes rule was compiled with a different Scala 3 minor ($majorMinorCompilerScalaVersion) " +
                s"than the target sources ($majorMinorScalaVersion). To fix this problem, make sure you are running the latest version of Scalafix. " +
                "If that is the case, either change your build to stick to the Scala 3 LTS or Next versions supported by Scalafix, or " +
                "enable ExplicitResultTypes.fetchScala3CompilerArtifactsOnVersionMismatch in .scalafix.conf in order to try to load what is needed dynamically."
            )
          } else {
            val pcTypeInferrer =
              if (matchingMinors)
                PresentationCompilerTypeInferrer.static(
                  config,
                  new ScalaPresentationCompiler()
                )
              else
                PresentationCompilerTypeInferrer.dynamic(config)

            Configured.Ok(new ExplicitResultTypes(conf, Some(pcTypeInferrer)))
          }
      }
    }
  }

  override def fix(implicit ctx: SemanticDocument): Patch =
    try {
      implicit val printer = new Scala3Printer(pcTypeInferrer)
      unsafeFix()
    } catch {
      case _: Throwable if !config.fatalWarnings =>
        Patch.empty
    }

}

class Scala3Printer(
    pcTypeInferrer: Option[PresentationCompilerTypeInferrer]
) extends Printer {

  private def defnBody(defn: Defn): Option[Term] = Option(defn).collect {
    case Defn.Val(_, _, _, term) => term
    case Defn.Var(_, _, _, Some(term)) => term
    case Defn.Def(_, _, _, _, _, term) => term
  }

  /**
   * Detects if the body of a definition refers to a Java enum constant. Returns
   * Some((constantName, enumClassName)) if so, None otherwise.
   *
   * Starting with Scala 3.8.2, the presentation compiler returns singleton
   * types (e.g. `DISPLAY.type`) for Java enum constants instead of the widened
   * enum class type (e.g. `Category`). This mirrors what Scala 2 handles in
   * CompilerTypePrinter.preProcess via JAVA_ENUM flag handling.
   */
  private def javaEnumWidening(
      defn: Defn
  )(implicit ctx: SemanticDocument): Option[(String, String)] = {
    defnBody(defn).flatMap { body =>
      val sym = body.symbol
      sym.info.flatMap { si =>
        if (si.isJava && si.isEnum) {
          sym.owner.info.map { ownerInfo =>
            (si.displayName, ownerInfo.displayName)
          }
        } else None
      }
    }
  }

  def defnType(
      defn: Defn,
      replace: Token,
      space: String
  )(implicit
      ctx: SemanticDocument
  ): Option[Patch] = {
    pcTypeInferrer.flatMap(_.defnType(replace, javaEnumWidening(defn)))
  }
}
