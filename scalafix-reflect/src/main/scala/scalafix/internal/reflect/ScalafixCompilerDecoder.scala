package scalafix.internal.reflect

import java.io.FileNotFoundException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import metaconfig.Input
import scalafix.rule.Rule
import scalafix.internal.config.LazySemanticdbIndex
import scalafix.internal.config.ScalafixMetaconfigReaders.UriRule
import scalafix.internal.util.FileOps
import metaconfig.Conf
import metaconfig.ConfDecoder
import metaconfig.ConfError
import metaconfig.Configured
import metaconfig.Configured.NotOk
import metaconfig.Configured.Ok
import org.langmeta.io.AbsolutePath

object ScalafixCompilerDecoder {
  def baseCompilerDecoder(index: LazySemanticdbIndex): ConfDecoder[Rule] = {
    implicit val cwd: AbsolutePath = index.workingDirectory
    ConfDecoder.instance[Rule] {
      case FromSourceRule(rule) =>
        rule match {
          case Ok(code) => ScalafixToolbox.getRule(code, index)
          case err @ NotOk(_) => err
        }
    }
  }

  object UrlRule {
    def unapply(arg: Conf.Str): Option[Configured[URL]] = arg match {
      case UriRule("http" | "https", uri) if uri.isAbsolute =>
        Option(Ok(uri.toURL))
      case GitHubUrlRule(url) => Option(url)
      case _ => None
    }
  }

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

  object FileRule {
    def unapply(arg: Conf.Str)(
        implicit cwd: AbsolutePath): Option[AbsolutePath] =
      arg match {
        case UriRule("file", uri) =>
          val path = AbsolutePath(Paths.get(uri.getSchemeSpecificPart))
          Option(path)
        case _ => None
      }
  }

  object FromSourceRule {
    private val scalafixRoot = Files.createTempDirectory("scalafix")
    scalafixRoot.toFile.deleteOnExit()
    private val fileCache = scala.collection.concurrent.TrieMap.empty[Int, Path]
    private def getTempFile(url: URL, code: String): Path =
      fileCache.getOrElseUpdate(
        code.hashCode, {
          val filename = Paths.get(url.getPath).getFileName.toString
          val tmp = Files.createTempFile(scalafixRoot, filename, ".scala")
          Files.write(tmp, code.getBytes)
          tmp
        }
      )
    def unapply(arg: Conf.Str)(
        implicit cwd: AbsolutePath): Option[Configured[Input]] = arg match {
      case FileRule(file) =>
        // NOgg
        Option(Ok(Input.File(file.toNIO)))
      case UrlRule(Ok(url)) =>
        try {
          val code = FileOps.readURL(url)
          val file = getTempFile(url, code)
          Option(Ok(Input.File(file)))
        } catch {
          case e: FileNotFoundException =>
            Option(Configured.error(s"404 - not found $url"))
        }
      case _ => None
    }
  }

}
