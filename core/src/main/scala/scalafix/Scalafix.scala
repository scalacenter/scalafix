package scalafix

import scalafix.rewrite.Rewrite

object Scalafix {

  def fix(code: String, rewrites: Seq[Rewrite]): String = {
    rewrites.foldLeft(code) {
      case (newCode, rewrite) =>
        rewrite.rewrite(newCode)
    }
  }
}
