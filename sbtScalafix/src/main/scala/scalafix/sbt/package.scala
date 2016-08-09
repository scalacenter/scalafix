package scalafix

package object sbt {
  type ScalafixLike = {
    def fix(code: String, filename: String): String
  }
}

