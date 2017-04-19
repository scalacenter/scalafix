package scalafix.reflect

import scala.meta._
import scalafix.Rewrite
import scalafix.config._
import scalafix.rewrite.ScalafixMirror
import scalafix.util.FileOps

import java.io.File
import java.net.URL

import metaconfig.Conf
import metaconfig.ConfDecoder

object ScalafixCompilerDecoder {
  def apply(mirror: Option[ScalafixMirror]): ConfDecoder[Rewrite] =
    ConfDecoder.instance[Rewrite] {
      case FromSourceRewrite(code) =>
        ScalafixToolbox.getRewrite(code, mirror)
      case els =>
        rewriteConfDecoder(mirror).read(els)
    }

  object UrlRewrite {
    def unapply(arg: Conf.Str): Option[URL] = arg match {
      case UriRewrite("http" | "https", uri) if uri.isAbsolute =>
        Option(uri.toURL)
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
    def unapply(arg: Conf.Str): Option[Input] = arg match {
      case FileRewrite(file) => Option(Input.File(file))
      case UrlRewrite(url) =>
        val code = FileOps.readURL(url)
        val file = File.createTempFile(url.toString, ".scala")
        FileOps.writeFile(file, code)
        Option(Input.File(file))
      case _ => None
    }
  }

}
