package scalafix.internal.interfaces

import java.nio.file.Path
import java.util.Optional

import scalafix.cli.ExitStatus
import scalafix.interfaces.{
  ScalafixDiagnostic,
  ScalafixError,
  ScalafixFileEvaluation,
  ScalafixPatch,
  ScalafixRule
}
import scalafix.internal.diff.DiffUtils
import scalafix.internal.v1.{MainOps, ValidatedArgs}
import scalafix.lint.RuleDiagnostic
import scalafix.internal.util.OptionOps._
import scalafix.v0
import scalafix.v0.RuleCtx

import scala.meta.io.AbsolutePath

final case class ScalafixFileEvaluationImpl(
    originalPath: AbsolutePath,
    fixedOpt: Option[String],
    error: ExitStatus,
    errorMessage: Option[String],
    diagnostics: Seq[RuleDiagnostic],
    patches: Seq[ScalafixPatchImpl]
)(
    args: ValidatedArgs,
    ctxOpt: Option[RuleCtx],
    index: Option[v0.SemanticdbIndex]
) extends ScalafixFileEvaluation {
  val rules: Seq[ScalafixRuleImpl] =
    args.rules.rules.map(new ScalafixRuleImpl(_))
  val pathToReplace = args.pathReplace(originalPath)

  override def getEvaluatedPath: Path = originalPath.toNIO

  override def getEvaluatedRules: Array[ScalafixRule] = rules.toArray

  override def previewPatches(): Optional[String] = fixedOpt.asJava

  override def getUnifiedDiff: Optional[String] = {
    fixedOpt
      .flatMap(fixed => {
        val input = args.input(originalPath).text
        if (input == fixed) None
        else
          Some(
            DiffUtils.unifiedDiff(
              originalPath.toString(),
              "<expected fix>",
              input.linesIterator.toList,
              fixed.linesIterator.toList,
              3
            )
          )
      })
      .asJava
  }

  override def getErrors(): Array[ScalafixError] =
    ScalafixErrorImpl.fromScala(error)

  override def isSuccessful: Boolean = error.isOk

  override def getDiagnostics: Array[ScalafixDiagnostic] =
    diagnostics.map(ScalafixDiagnosticImpl.fromScala).toArray

  override def getPatches: Array[ScalafixPatch] = {
    patches.toArray
  }

  override def applyPatches(): Array[ScalafixError] = {
    val exitStatus = fixedOpt.toTry
      .flatMap(MainOps.applyDiff(args, file = originalPath, _))
      .getOrElse(ExitStatus.UnexpectedError)
    ScalafixErrorImpl.fromScala(exitStatus).toSeq.toArray
  }

  override def previewPatches(
      selectedPatches: Array[ScalafixPatch]
  ): Optional[String] = {
    val selectedPatchesSet =
      new java.util.IdentityHashMap[ScalafixPatch, Unit](selectedPatches.length)
    for (patch <- selectedPatches)
      selectedPatchesSet.put(patch, ())
    val filteredPatches = patches.filter(selectedPatchesSet.containsKey(_))
    ctxOpt
      .flatMap(ctx =>
        MainOps.previewPatches(filteredPatches.map(_.patch), ctx, index)
      )
      .asJava
  }
  override def applyPatches(
      selectedPatches: Array[ScalafixPatch]
  ): Array[ScalafixError] = {
    val selectedPatchesSet =
      new java.util.IdentityHashMap[ScalafixPatch, Unit](selectedPatches.length)
    for (patch <- selectedPatches)
      selectedPatchesSet.put(patch, ())
    val filteredPatches = patches.filter(selectedPatchesSet.containsKey(_))
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
    ScalafixErrorImpl.fromScala(exitStatus)
  }

}

object ScalafixFileEvaluationImpl {

  def from(
      originalPath: AbsolutePath,
      fixed: Option[String],
      exitStatus: ExitStatus,
      patches: Seq[v0.Patch],
      diagnostics: Seq[RuleDiagnostic]
  )(
      args: ValidatedArgs,
      ctx: RuleCtx,
      index: Option[v0.SemanticdbIndex]
  ): ScalafixFileEvaluationImpl = {
    val scalafixPatches = patches.map(ScalafixPatchImpl)
    ScalafixFileEvaluationImpl(
      originalPath = originalPath,
      fixedOpt = fixed,
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
  ): ScalafixFileEvaluationImpl =
    ScalafixFileEvaluationImpl(
      originalPath,
      None,
      exitStatus,
      Some(errorMessage),
      Nil,
      Nil
    )(args, None, None)
}
