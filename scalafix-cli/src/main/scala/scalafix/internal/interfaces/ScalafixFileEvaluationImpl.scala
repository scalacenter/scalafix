package scalafix.internal.interfaces

import java.nio.file.Path
import java.util.Optional

import scala.meta.io.AbsolutePath

import scalafix.cli.ExitStatus
import scalafix.interfaces.ScalafixDiagnostic
import scalafix.interfaces.ScalafixError
import scalafix.interfaces.ScalafixFileEvaluation
import scalafix.interfaces.ScalafixFileEvaluationError
import scalafix.interfaces.ScalafixPatch
import scalafix.interfaces.ScalafixRule
import scalafix.internal.diff.DiffUtils
import scalafix.internal.util.OptionOps._
import scalafix.internal.v1.MainOps
import scalafix.internal.v1.ValidatedArgs
import scalafix.lint.RuleDiagnostic
import scalafix.v0
import scalafix.v0.RuleCtx

final case class ScalafixFileEvaluationImpl(
    originalPath: AbsolutePath,
    fixedOpt: Option[String],
    exitStatus: ExitStatus,
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
  val pathToReplace: AbsolutePath = args.pathReplace(originalPath)

  override def getEvaluatedFile: Path = originalPath.toNIO

  override def getEvaluatedRules: Array[ScalafixRule] = rules.toArray

  override def previewPatches(): Optional[String] = fixedOpt.asJava

  override def previewPatchesAsUnifiedDiff: Optional[String] = {
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

  override def getError(): Optional[ScalafixFileEvaluationError] =
    exitStatus match {
      case ExitStatus.Ok => None.asJava
      case ExitStatus.ParseError =>
        Some(ScalafixFileEvaluationError.ParseError).asJava
      case ExitStatus.MissingSemanticdbError =>
        Some(ScalafixFileEvaluationError.MissingSemanticdbError).asJava
      case ExitStatus.StaleSemanticdbError =>
        Some(ScalafixFileEvaluationError.StaleSemanticdbError).asJava
      case _ => Some(ScalafixFileEvaluationError.UnexpectedError).asJava
    }

  override def isSuccessful: Boolean = !getError().isPresent

  override def getErrorMessage: Optional[String] = errorMessage.asJava

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
      exitStatus = exitStatus,
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
