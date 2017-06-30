package scalafix

import metaconfig._, typesafeconfig._
import scala.meta.inputs.Input
import scalafix.config.ScalafixMetaconfigReaders
import scalafix.reflect.ScalafixReflect
import scalatags.Text.TypedTag
import scalatags.Text.all._
import scalatex.site.Highlighter

object Readme {
  val highlight = new Highlighter {}
  def note = b("Note. ")
  def gitter =
    raw(
      """<a href="https://gitter.im/scalacenter/scalafix?utm_source=badge&amp;utm_medium=badge&amp;utm_campaign=pr-badge&amp;utm_content=badge"><img src="https://camo.githubusercontent.com/382ebf95f5b4df9275ac203229928db8c8fd5c50/68747470733a2f2f6261646765732e6769747465722e696d2f6f6c6166757270672f7363616c61666d742e737667" alt="Join the chat at https://gitter.im/scalacenter/scalafix" data-canonical-src="https://badges.gitter.im/scalacenter/scalafix.svg" style="max-width:100%;"></a>""")
  def bintrayRepo =
    """resolvers += Resolver.bintrayRepo("scalameta", "maven")"""
  def github: String = "https://github.com"
  def repo: String = "https://github.com/scalacenter/scalafix"
  def metaRepo: String = "https://github.com/scalameta/scalameta"
  def user(name: String): Frag = a(href := s"$github/$name", s"@$name")
  def issue(i: Int): Frag = a(href := s"$repo/issues/$i", s"#$i")
  def metaIssue(i: Int): Frag = a(href := s"$metaRepo/issues/$i", s"#$i")
  def pr(i: Int): Frag = a(href := s"$repo/pulls/$i", s"#$i")
  def dotty = a(href := "http://dotty.epfl.ch/", "Dotty")
  def comment(frags: Frag*): TypedTag[String] = span("")
  def config(str: String): TypedTag[String] = {
    // assert that config listings in docs is free of typos.
    ScalafixMetaconfigReaders
      .scalafixConfigConfDecoder(ScalafixReflect.syntactic)
      .read(Input.String(str).toConf.get)
      .get
    highlight.scala(str)
  }
  def githubSyntax(path: String) =
    reflect.ScalafixReflect.GitHubUrlRewrite
      .unapply(Conf.Str(s"github:$path"))
      .get
}
