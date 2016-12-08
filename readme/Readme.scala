package scalafix

import scalatags.Text.TypedTag
import scalatags.Text.all._
import scalatex._
import scalatags.Text.all._

object Readme {
  def note = b("Note. ")
  def github: String = "https://github.com"
  def repo: String = "https://github.com/scalacenter/scalafix"
  def issue(i: Int): Frag = a(href := s"$repo/issues/$i", s"#$i")
  def dotty = a(href := "http://dotty.epfl.ch/", "Dotty")
  def comment(frags: Frag*): TypedTag[String] = span("")
  def config(str: String): TypedTag[String] = {
    // assert that config listings in docs is free of typos.
    val Right(_) = ScalafixConfig.fromString(str)
    pre(code(str))
  }
}
