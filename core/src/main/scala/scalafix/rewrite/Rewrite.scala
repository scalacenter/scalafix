package scalafix.rewrite

abstract class Rewrite {
  def rewrite(code: String): String
}
