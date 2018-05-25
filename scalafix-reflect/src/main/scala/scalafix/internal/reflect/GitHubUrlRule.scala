package scalafix.internal.reflect

import java.net.URL
import metaconfig.Conf
import metaconfig.ConfError
import metaconfig.Configured
import metaconfig.Configured.Ok

object GitHubUrlRule {
  private[this] val GitHubShorthand =
    """github:([^\/]+)\/([^\/]+)\/([^\/]+)""".r
  private[this] val GitHubShorthandWithSha =
    """github:([^\/]+)\/([^\/]+)\/([^\/]+)\?sha=(.+)""".r
  private[this] val GitHubFallback =
    """github:(.*)""".r

  private[this] val alphanumerical = "[^a-zA-Z0-9]"

  // approximates the "format=Camel" formatter in giter8.
  // http://www.foundweekends.org/giter8/Combined+Pages.html#Formatting+template+fields
  private[this] def CamelCase(string: String): String =
    string.split(alphanumerical).mkString.capitalize

  // approximates the "format=Snake" formatter in giter8.
  private[this] def SnakeCase(string: String): String =
    string.split(alphanumerical).map(_.toLowerCase).mkString("_")

  private[this] def expandGitHubURL(
      org: String,
      repo: String,
      version: String,
      sha: String): URL = {
    val fileName = s"${CamelCase(repo)}_${SnakeCase(version)}.scala"
    new URL(
      s"https://raw.githubusercontent.com/$org/$repo/$sha/scalafix/rules/src/main/scala/fix/$fileName")
  }

  def unapply(arg: Conf.Str): Option[Configured[URL]] = arg.value match {
    case GitHubShorthandWithSha(org, repo, version, sha) =>
      Option(Ok(expandGitHubURL(org, repo, version, sha)))
    case GitHubShorthand(org, repo, version) =>
      Option(Ok(expandGitHubURL(org, repo, version, "master")))
    case GitHubFallback(invalid) =>
      Some(
        ConfError
          .message(s"""Invalid url 'github:$invalid'. Valid formats are:
                      |- github:org/repo/version
                      |- github:org/repo/version?sha=branch""".stripMargin)
          .notOk)
    case _ => None
  }
}
