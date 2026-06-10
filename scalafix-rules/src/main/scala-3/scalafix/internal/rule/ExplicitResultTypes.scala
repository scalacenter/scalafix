package scalafix.internal.rule

import scala.util.Failure
import scala.util.Success

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

  private def rootCause(error: Throwable): Throwable = {
    var cause = error
    while (cause.getCause != null) {
      cause = cause.getCause
    }
    cause
  }

  private def dynamicPresentationCompilerError(
      compilerScalaVersion: String,
      error: Throwable
  ): String = {
    val cause = rootCause(error)
    val detail =
      Option(cause.getMessage).filter(_.nonEmpty) match {
        case Some(message) => s"${cause.getClass.getSimpleName}: $message"
        case None => cause.getClass.getSimpleName
      }

    s"The ExplicitResultTypes rule could not fetch or load the presentation compiler for the Scala version $compilerScalaVersion " +
      s"which was used to compile the target sources. Cause: $detail. To fix this problem, load Scalafix with the Scala minor version " +
      s"${stripPatchVersion(compilerScalaVersion)} to remove the need for fetchScala3CompilerArtifactsOnVersionMismatch, or try to upgrade " +
      s"Scalafix to the latest version."
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
            if (matchingMinors) {
              val pcTypeInferrer =
                PresentationCompilerTypeInferrer.static(
                  config,
                  new ScalaPresentationCompiler()
                )

              Configured.Ok(new ExplicitResultTypes(conf, Some(pcTypeInferrer)))
            } else {
              PresentationCompilerTypeInferrer.dynamic(config) match {
                case Success(pcTypeInferrer) =>
                  Configured.Ok(
                    new ExplicitResultTypes(conf, Some(pcTypeInferrer))
                  )
                case Failure(error) =>
                  Configured.error(
                    dynamicPresentationCompilerError(
                      config.scalaVersion,
                      error
                    )
                  )
              }
            }
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

  def defnType(
      defn: Defn,
      replace: Token,
      space: String
  )(implicit
      ctx: SemanticDocument
  ): Patch =
    pcTypeInferrer.fold(Patch.empty)(_.defnType(replace))
}
