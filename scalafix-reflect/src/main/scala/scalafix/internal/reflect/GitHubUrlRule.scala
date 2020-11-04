package scalafix.internal.reflect

import java.io.FileNotFoundException
import java.net.URL

import metaconfig.Conf
import metaconfig.ConfError
import metaconfig.Configured
import metaconfig.Configured.Ok

object GitHubUrlRule {

  def unapply(arg: Conf.Str): Option[Configured[URL]] = arg.value match {
    case GitHubOrgRepoVersionSha(org, repo, version, sha) =>
      Option(Ok(guessGitHubURL(org, repo, version, sha)))
    case GitHubOrgRepoVersion(org, repo, version) =>
      Option(Ok(guessGitHubURL(org, repo, version, "master")))
    case GitHubOrgRepo(org, repo) =>
      Option(Ok(guessGitHubURL(org, repo, normalCamelCase(repo), "master")))
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
      filename: String,
      sha: String
  ): URL = {
    val firstGuess = expandGitHubURL(org, repo, filename, sha)
    if (is404(firstGuess)) {
      val secondGuess = expandGitHubURL(org, repo, g8CamelCase(filename), sha)
      if (is404(secondGuess)) firstGuess
      else secondGuess
    } else {
      firstGuess
    }
  }

  private val GitHubOrgRepo =
    """github:([^\/]+)\/([^\/]+)""".r
  private val GitHubOrgRepoVersion =
    """github:([^\/]+)\/([^\/]+)\/([^\/]+)""".r
  private val GitHubOrgRepoVersionSha =
    """github:([^\/]+)\/([^\/]+)\/([^\/]+)\?sha=(.+)""".r
  private val GitHubFallback =
    """github:(.*)""".r

  private val alphanumerical = "[^a-zA-Z0-9]"

  // approximates the "format=Camel" formatter in giter8.
  // http://www.foundweekends.org/giter8/Combined+Pages.html#Formatting+template+fields
  // toLowerCase is required to fix https://github.com/scalacenter/scalafix/issues/342
  private def g8CamelCase(string: String): String =
    string.split(alphanumerical).mkString.toLowerCase.capitalize

  private def normalCamelCase(string: String): String =
    string.split(alphanumerical).map(_.capitalize).mkString

  private def is404(url: URL): Boolean = {
    try {
      url.openStream().close()
      false
    } catch {
      case _: FileNotFoundException =>
        true
    }
  }
  private def expandGitHubURL(
      org: String,
      repo: String,
      filename: String,
      sha: String
  ): URL = {
    new URL(
      s"https://raw.githubusercontent.com/$org/$repo/$sha/scalafix/rules/src/main/scala/fix/$filename.scala"
    )
  }

}
