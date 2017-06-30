package scalafix.internal.reflect

import java.io.File
import java.net.URL
import scala.meta.Input
import scalafix.Rewrite
import scalafix.config.LazyMirror
import scalafix.config.ScalafixMetaconfigReaders.UriRewrite
import scalafix.internal.util.FileOps
import metaconfig.Conf
import metaconfig.ConfDecoder
import metaconfig.ConfError
import metaconfig.Configured
import metaconfig.Configured.NotOk
import metaconfig.Configured.Ok

object ScalafixCompilerDecoder {
  def baseCompilerDecoder(mirror: LazyMirror): ConfDecoder[Rewrite] =
    ConfDecoder.instance[Rewrite] {
      case FromSourceRewrite(rewrite) =>
        rewrite match {
          case Ok(code) => ScalafixToolbox.getRewrite(code, mirror)
          case err @ NotOk(_) => err
        }
    }

  object UrlRewrite {
    def unapply(arg: Conf.Str): Option[Configured[URL]] = arg match {
      case UriRewrite("http" | "https", uri) if uri.isAbsolute =>
        Option(Ok(uri.toURL))
      case GitHubUrlRewrite(url) => Option(url)
      case _ => None
    }
  }

  object GitHubUrlRewrite {
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

    private[this] def expandGitHubURL(org: String,
                                      repo: String,
                                      version: String,
                                      sha: String): URL = {
      val fileName = s"${CamelCase(repo)}_${SnakeCase(version)}.scala"
      new URL(
        s"https://raw.githubusercontent.com/$org/$repo/$sha/scalafix/rewrites/src/main/scala/fix/$fileName")
    }

    def unapply(arg: Conf.Str): Option[Configured[URL]] = arg.value match {
      case GitHubShorthandWithSha(org, repo, version, sha) =>
        Option(Ok(expandGitHubURL(org, repo, version, sha)))
      case GitHubShorthand(org, repo, version) =>
        Option(Ok(expandGitHubURL(org, repo, version, "master")))
      case GitHubFallback(invalid) =>
        Some(
          ConfError
            .msg(s"""Invalid url 'github:$invalid'. Valid formats are:
                    |- github:org/repo/version
                    |- github:org/repo/version?sha=branch""".stripMargin)
            .notOk)
      case _ => None
    }
  }

  object FileRewrite {
    def unapply(arg: Conf.Str): Option[File] = arg match {
      case UriRewrite("file", uri) =>
        Option(new File(uri.getSchemeSpecificPart).getAbsoluteFile)
      case _ => None
    }
  }

  object FromSourceRewrite {
    def unapply(arg: Conf.Str): Option[Configured[Input]] = arg match {
      case FileRewrite(file) =>
        // NOgg
        Option(Ok(Input.File(file)))
      case UrlRewrite(Ok(url)) =>
        val code = FileOps.readURL(url)
        val file = File.createTempFile(url.toString, ".scala")
        FileOps.writeFile(file, code)
        Option(Ok(Input.File(file)))
      case _ => None
    }
  }

}
