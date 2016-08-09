package scalafix

import scalatags.Text.TypedTag
import scalatags.Text.all._

object Readme {
  def github: String = "https://github.com"
  def repo: String = "https://github.com/scalacenter/scalafix"
  def dotty = a(href := "http://dotty.epfl.ch/", "Dotty")

}
