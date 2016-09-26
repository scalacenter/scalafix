package scalafix

package object sbt {
  type ScalafixLike = {
    def main(args: Array[String]): Unit
  }
}
