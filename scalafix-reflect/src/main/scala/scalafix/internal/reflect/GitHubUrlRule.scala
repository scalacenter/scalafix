package scalafix.internal.reflect

import java.io.FileNotFoundException
import java.net.URL
import metaconfig.Conf
import metaconfig.ConfError
import metaconfig.Configured
import metaconfig.Configured.Ok

import scala.util.Try

object GitHubUrlRule {

  private val DefaultBranch = "master"

  def unapply(arg: Conf.Str): Option[Configured[URL]] = arg.value match {
    case GitHubOrgRepoVersionSha(org, repo, rule, sha) =>
      Some(Ok(guessGitHubURL(org, repo, rule, sha)))
    case GitHubOrgRepoVersion(org, repo, rule) =>
      Some(Ok(guessGitHubURL(org, repo, rule, DefaultBranch)))
    case GitHubOrgRepo(org, repo) =>
      Some(Ok(guessGitHubURL(org, repo, normalCamelCase(repo), DefaultBranch)))
    case GitHubFallback(invalid) =>
      Some(
        ConfError
          .message(s"""Invalid url 'github:$invalid'. Valid formats are:
            |- github:org/repo
            |- github:org/repo/name
            |- github:org/repo/name?sha=branch""".stripMargin)
          .notOk
      )
    case _ => None
  }

  private def guessGitHubURL(
      org: String,
      repo: String,
      rule: String,
      sha: String
  ): URL = {
    val (path, name) = rule.split("\\.").toList match {
      case name :: Nil => ("", name)
      case p :+ name => (p.mkString("", "/", "/"), name)
    }
    val file = path + name + ".scala"
    val url = expandGitHubURL(org, repo, file, sha)
    checkUrl(url)
      .recoverWith { case _: FileNotFoundException =>
        val fallbackFile = path + g8CamelCase(name) + ".scala"
        checkUrl(expandGitHubURL(org, repo, fallbackFile, sha))
      }
      .getOrElse(url)
  }

  private val GitHubOrgRepo =
    """github:([^\/]+)\/([^\/]+)""".r
  private val GitHubOrgRepoVersion =
    """github:([^\/]+)\/([^\/]+)\/([^\/]+)""".r
  private val GitHubOrgRepoVersionSha =
    """github:([^\/]+)\/([^\/]+)\/([^\/]+)\?sha=(.+)""".r
  private val GitHubFallback =
    """github:(.*)""".r

  private val NonAlphaNumeric = "[^a-zA-Z0-9]"

  // approximates the "format=Camel" formatter in giter8.
  // http://www.foundweekends.org/giter8/Combined+Pages.html#Formatting+template+fields
  // toLowerCase is required to fix https://github.com/scalacenter/scalafix/issues/342
  private def g8CamelCase(string: String): String =
    string.split(NonAlphaNumeric).mkString.toLowerCase.capitalize

  private def normalCamelCase(string: String): String =
    string.split(NonAlphaNumeric).map(_.capitalize).mkString

  private def checkUrl(url: URL): Try[URL] =
    Try(url.openStream().close()).map(_ => url)

  private def expandGitHubURL(
      org: String,
      repo: String,
      file: String,
      sha: String
  ): URL = new URL(
    s"https://raw.githubusercontent.com/$org/$repo/$sha/scalafix/rules/src/main/scala/fix/$file"
  )

}
