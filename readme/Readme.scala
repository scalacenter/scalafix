package scalafix

import scalatags.Text.all._

import scalatex._
import scalatags.Text.all._

object Readme {
  def github: String = "https://github.com"
  def repo: String = "https://github.com/scalacenter/scalafix"
  def issue(i: Int): Frag = a(href:=s"$repo/issues/$i", s"#$i")
  def dotty = a(href := "http://dotty.epfl.ch/", "Dotty")

}
