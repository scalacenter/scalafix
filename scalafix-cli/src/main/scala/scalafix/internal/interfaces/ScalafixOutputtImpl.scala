package scalafix.internal.interfaces

import java.lang
import java.nio.file.Path
import java.util.Optional

import scalafix.v1.Rule
import scalafix.cli.ExitStatus
import scalafix.interfaces.{
  ScalafixDiagnostic,
  ScalafixError,
  ScalafixOutput,
  ScalafixPatch,
  ScalafixRule
}
import scalafix.internal.v1.{MainOps, Rules, ValidatedArgs}
import scalafix.lint.RuleDiagnostic
import scalafix.internal.util.OptionOps._
import scalafix.internal.v0.{LegacyRuleCtx, LegacySemanticdbIndex}
import scalafix.v0
import scalafix.v0.RuleCtx

import scala.meta.io.AbsolutePath

final case class ScalafixOutputtImpl(
    originalPath: AbsolutePath,
    fixed: Option[String],
    unifiedDiff: Option[String],
    error: ExitStatus,
    errorMessage: Option[String],
    diagnostics: Seq[RuleDiagnostic],
    patches: Seq[ScalafixPatchImpl]
)(
    args: ValidatedArgs,
    ctxOpt: Option[RuleCtx],
    index: Option[v0.SemanticdbIndex]
) extends ScalafixOutput {

  val rules: Seq[ScalafixRuleImpl] =
    args.rules.rules.map(new ScalafixRuleImpl(_))
  val pathToReplace = args.pathReplace(originalPath)

  override def getPath: Path = originalPath.toNIO

  override def getRules: Array[ScalafixRule] = rules.toArray

  override def getOutputFileFixed(): Optional[String] = fixed.asJava

  override def getUnifiedDiff: Optional[String] = unifiedDiff.asJava

  override def getError(): Optional[ScalafixError] =
    ScalafixErrorImpl.fromScala(error).asJava

  override def isSuccessful: lang.Boolean = error.isOk

  override def getDiagnostics: Array[ScalafixDiagnostic] =
    diagnostics.map(ScalafixDiagnosticImpl.fromScala).toArray

  override def getPatches: Array[ScalafixPatch] = {
    patches.toArray
  }

  override def applyPatches(): Array[ScalafixError] = {
    val exitStatus = fixed.toTry
      .flatMap(MainOps.appyDiff(args, file = originalPath, _))
      .getOrElse(ExitStatus.UnexpectedError)
    ScalafixErrorImpl.fromScala(exitStatus).toSeq.toArray
  }

  override def applySelectivePatches(
      selectedPatches: Array[ScalafixPatch]
  ): Array[ScalafixError] = {
    val ids = selectedPatches.toList.map(_.getId())
    val filteredPatches = patches.filter {
      case ScalafixPatchImpl(id, _) => ids.contains(id.value)
    }
    val exitStatus =
      ctxOpt.toTry
        .flatMap(ctx =>
          MainOps.applyPatches(
            args,
            filteredPatches.map(_.patch),
            ctx,
            index,
            pathToReplace
          )
        )
        .getOrElse(ExitStatus.UnexpectedError)
    ScalafixErrorImpl.fromScala(exitStatus).toSeq.toArray
  }

}

object ScalafixOutputtImpl {

  def from(
      originalPath: AbsolutePath,
      fixed: Option[String],
      unifiedDiff: Option[String],
      exitStatus: ExitStatus,
      patches: Seq[v0.Patch],
      diagnostics: Seq[RuleDiagnostic]
  )(
      args: ValidatedArgs,
      ctx: RuleCtx,
      index: Option[v0.SemanticdbIndex]
  ): ScalafixOutputtImpl = {
    val scalafixPatches = patches.map(ScalafixPatchImpl.from)
    ScalafixOutputtImpl(
      originalPath = originalPath,
      fixed = fixed,
      unifiedDiff = unifiedDiff,
      error = exitStatus,
      errorMessage = None,
      diagnostics = diagnostics,
      patches = scalafixPatches
    )(args, Some(ctx), index)
  }
  def from(
      originalPath: AbsolutePath,
      exitStatus: ExitStatus,
      errorMessage: String
  )(
      args: ValidatedArgs
  ): ScalafixOutputtImpl =
    ScalafixOutputtImpl(
      originalPath,
      None,
      None,
      exitStatus,
      Some(errorMessage),
      Nil,
      Nil
    )(args, None, None)
}
