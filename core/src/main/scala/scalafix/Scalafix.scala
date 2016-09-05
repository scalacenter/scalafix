package scalafix

import scala.meta.inputs.Input
import scala.util.control.NonFatal
import scalafix.rewrite.Rewrite

object Scalafix {
  def fix(code: String, rewriters: Seq[Rewrite] = Rewrite.default): Fixed = {
    fix(Input.String(code), rewriters)
  }

  def fix(code: Input, rewriters: Seq[Rewrite]): Fixed = {
    rewriters.foldLeft[Fixed](
      Fixed.Success(String.copyValueOf(code.chars))) {
      case (newCode: Fixed.Success, rewriter) =>
        try rewriter.rewrite(Input.String(newCode.code))
        catch {
          case NonFatal(e) => Fixed.Failure(e)
        }
      case (failure, _) => failure
    }
  }
}
