package scalafix.internal.reflect

import java.io.FileNotFoundException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import metaconfig.Conf
import metaconfig.Configured
import metaconfig.Configured.Ok
import metaconfig.Input
import scala.collection.concurrent.TrieMap
import scala.meta.io.AbsolutePath
import scalafix.internal.config.ScalafixMetaconfigReaders.UriRule
import scalafix.internal.util.FileOps
import scalafix.internal.v0.LegacySemanticRule
import scalafix.internal.v0.LegacySyntacticRule
import scalafix.v0
import scalafix.v1

object RuleDecoderOps {

  val legacySemanticRuleClass: Class[v0.SemanticRule] =
    classOf[scalafix.rule.SemanticRule]
  val legacyRuleClass: Class[v0.Rule] =
    classOf[scalafix.rule.Rule]
  def toRule(cls: Class[_]): v1.Rule = {
    assertNotOutdatedScalafixRule(cls)
    if (legacySemanticRuleClass.isAssignableFrom(cls)) {
      val fn: v0.SemanticdbIndex => v0.Rule = { index =>
        val ctor = cls.getDeclaredConstructor(classOf[v0.SemanticdbIndex])
        ctor.setAccessible(true)
        ctor.newInstance(index).asInstanceOf[v0.Rule]
      }
      new LegacySemanticRule(fn(v0.SemanticdbIndex.empty).name, fn)
    } else if (legacyRuleClass.isAssignableFrom(cls)) {
      val ctor = cls.getDeclaredConstructor()
      ctor.setAccessible(true)
      new LegacySyntacticRule(ctor.newInstance().asInstanceOf[v0.Rule])
    } else {
      val ctor = cls.getDeclaredConstructor()
      ctor.setAccessible(true)
      cls.newInstance().asInstanceOf[v1.Rule]
    }
  }

  def assertNotOutdatedScalafixRule(cls: Class[_]): Unit = {
    cls.getSuperclass.getName match {
      case "scalafix.rule.Rule" | "scalafix.rule.SemanticRule" =>
        // Custom rule is using 0.5 API that is not supported here.
        throw new IllegalArgumentException(
          "Outdated Scalafix rule, please upgrade to the latest Scalafix version"
        )
      case _ =>
        ()
    }
  }

  def tryClassload(classloader: ClassLoader, fqn: String): Option[v1.Rule] = {
    try {
      Some(toRule(classloader.loadClass(fqn)))
    } catch {
      case _: ClassNotFoundException | _: NoSuchMethodException =>
        try {
          Some(toRule(classloader.loadClass(fqn + "$")))
        } catch {
          case _: ClassNotFoundException =>
            None
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

  // Warning: evil global mutable state
  val scalafixRoot: Path = Files.createTempDirectory("scalafix")
  scalafixRoot.toFile.deleteOnExit()
  val fileCache: TrieMap[Int, Path] =
    scala.collection.concurrent.TrieMap.empty[Int, Path]

  class FromSourceRule(cwd: AbsolutePath) {
    private def getTempFile(url: URL, code: String): Path =
      fileCache.getOrElseUpdate(
        code.hashCode, {
          val filename = Paths.get(url.getPath).getFileName.toString
          val tmp = Files.createTempFile(scalafixRoot, filename, ".scala")
          Files.write(tmp, code.getBytes)
          tmp
        }
      )
    def unapply(arg: Conf.Str): Option[Configured[Input]] = arg match {
      case UriRule("file", uri) =>
        val path = AbsolutePath(Paths.get(uri.getSchemeSpecificPart))(cwd)
        Option(Ok(Input.File(path.toNIO)))
      case UrlRule(Ok(url)) =>
        try {
          val code = FileOps.readURL(url)
          val file = getTempFile(url, code)
          Option(Ok(Input.File(file)))
        } catch {
          case _: FileNotFoundException =>
            Option(Configured.error(s"404 - not found $url"))
        }
      case _ => None
    }
  }
}
