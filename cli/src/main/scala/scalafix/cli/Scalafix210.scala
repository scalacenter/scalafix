package scalafix.cli

import scalafix.Fixed
import scalafix.Scalafix
import scalafix.rewrite.Rewrite
import scalafix.util.logger

class Scalafix210 {
  def fix(originalContents: String, filename: String): String = {
    Scalafix.fix(originalContents, Rewrite.default) match {
      case Fixed.Success(fixedCode) => fixedCode
      case Fixed.Failure(e) =>
        logger.warn(s"Failed to fix $filename. Cause ${e.getMessage}")
        originalContents
    }
  }
}
