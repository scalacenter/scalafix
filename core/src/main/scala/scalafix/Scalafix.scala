package scalafix

import scala.meta.inputs.Input
import scala.util.control.NonFatal
import scalafix.rewrite.Rewrite

object Scalafix {
  def fix(code: String, rewriters: Seq[Rewrite] = Rewrite.default): FixResult = {
    fix(Input.String(code), rewriters)
  }

  def fix(code: Input, rewriters: Seq[Rewrite]): FixResult = {
    rewriters.foldLeft[FixResult](
        FixResult.Success(String.copyValueOf(code.chars))) {
      case (newCode: FixResult.Success, rewriter) =>
        try rewriter.rewrite(Input.String(newCode.code))
        catch {
          case NonFatal(e) => FixResult.Failure(e)
        }
      case (failure, _) => failure
    }
  }
}
