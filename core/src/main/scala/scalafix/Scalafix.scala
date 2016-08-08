package scalafix

import scalafix.rewrite.Rewrite

object Scalafix {
  def fix(code: String, rewriters: Seq[Rewrite]): FixResult = {
    rewriters.foldLeft[FixResult](FixResult.Success(code)) {
      case (newCode: FixResult.Success, rewriter) =>
        rewriter.rewrite(newCode.code)
      case (failure, _) => failure
    }
  }
}
