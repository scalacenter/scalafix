package scalafix.config

import scala.meta.Ref
import scala.meta._
import scala.meta.parsers.Parse
import scala.meta.semantic.v1.Symbol
import scala.reflect.ClassTag
import scala.util.Try
import scala.util.matching.Regex
import scalafix.rewrite.ScalafixMirror
import scalafix.rewrite.ScalafixRewrite
import scalafix.rewrite.ScalafixRewrites
import scalafix.util.ClassloadObject
import scalafix.util.FileOps
import scalafix.util.ScalafixToolbox
import scalafix.util.TreePatch._

import java.io.File
import java.net.URI
import java.net.URL

import metaconfig.Conf
import metaconfig.ConfDecoder
import org.scalameta.logger

object ScalafixMetaconfigReaders extends ScalafixMetaconfigReaders
// A collection of metaconfig.Reader instances that are shared across
trait ScalafixMetaconfigReaders {
  implicit lazy val parseReader: ConfDecoder[MetaParser] = {
    import scala.meta.parsers.Parse._
    ReaderUtil.oneOf[MetaParser](parseSource, parseStat, parseCase)
  }
  implicit lazy val dialectReader: ConfDecoder[Dialect] = {
    import scala.meta.dialects._
    ReaderUtil.oneOf[Dialect](Scala211, Sbt0137, Dotty, Paradise211)
  }

  object ClassloadRewrite {
    def unapply(arg: Conf.Str): Option[String] = arg match {
      case UriRewrite("scala", uri) =>
        val suffix = if (arg.value.endsWith("$")) "" else "$"
        Option(uri.getSchemeSpecificPart + suffix)
      case _ => None
    }
  }

  object UriRewrite {
    def unapply(arg: Conf.Str): Option[(String, URI)] =
      for {
        uri <- Try(new URI(arg.value)).toOption
        scheme <- Option(uri.getScheme)
      } yield scheme -> uri
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
    def unapply(arg: Conf.Str): Option[String] = arg match {
      case FileRewrite(file) => Option(FileOps.readFile(file))
      case UrlRewrite(url) => Option(FileOps.readURL(url))
      case _ => None
    }
  }

  implicit lazy val rewriteReader: ConfDecoder[ScalafixRewrite] =
    ConfDecoder.instance[ScalafixRewrite] {
      case ClassloadRewrite(fqn) =>
        ClassloadObject[ScalafixRewrite](fqn)
      case FromSourceRewrite(code) =>
        ScalafixToolbox.getRewrite[ScalafixMirror](code)
      case els =>
        ReaderUtil.fromMap(ScalafixRewrites.name2rewrite).read(els)
    }

  object ConfStrLst {
    def unapply(arg: Conf.Lst): Option[List[String]] =
      if (arg.values.forall(_.isInstanceOf[Conf.Str]))
        Some(arg.values.collect { case Conf.Str(value) => value })
      else None
  }

  implicit val RegexReader: ConfDecoder[Regex] = ConfDecoder.instance[Regex] {
    case Conf.Str(str) => Right(FilterMatcher.mkRegexp(List(str)))
    case ConfStrLst(values) => Right(FilterMatcher.mkRegexp(values))
  }
  private val fallbackFilterMatcher = FilterMatcher(Nil, Nil)
  implicit val FilterMatcherReader: ConfDecoder[FilterMatcher] =
    ConfDecoder.instance[FilterMatcher] {
      case Conf.Str(str) => Right(FilterMatcher(str))
      case ConfStrLst(values) =>
        Right(FilterMatcher(values, Nil))
      case els => fallbackFilterMatcher.reader.read(els)
    }

  def parseReader[T](implicit parse: Parse[T]): ConfDecoder[T] =
    ConfDecoder.stringR.flatMap { str =>
      str.parse[T] match {
        case parsers.Parsed.Success(x) => Right(x)
        case parsers.Parsed.Error(_, x, _) =>
          Left(new IllegalArgumentException(x))
      }
    }

  def castReader[From, To](ConfDecoder: ConfDecoder[From])(
      implicit ev: ClassTag[To]): ConfDecoder[To] = ConfDecoder.flatMap {
    case x if ev.runtimeClass.isInstance(x) => Right(x.asInstanceOf[To])
    case x =>
      Left(new IllegalArgumentException(s"Expected Ref, got ${x.getClass}"))
  }
  implicit lazy val importerReader: ConfDecoder[Importer] =
    parseReader[Importer]
  implicit lazy val refReader: ConfDecoder[Ref] =
    castReader[Stat, Ref](parseReader[Stat])
  implicit lazy val termRefReader: ConfDecoder[Term.Ref] =
    castReader[Stat, Term.Ref](parseReader[Stat])
  protected[scalafix] val fallbackReplace = Replace(Symbol.None, q"IGNOREME")
  implicit lazy val replaceReader: ConfDecoder[Replace] =
    fallbackReplace.reader
  implicit lazy val symbolReader: ConfDecoder[Symbol] =
    ConfDecoder.stringR.map(Symbol.apply)
  implicit def listReader[T: ConfDecoder]: ConfDecoder[List[T]] =
    ConfDecoder.instance[List[T]] {
      case x @ Conf.Lst(vs) =>
//        logger.elem(vs)
        ConfDecoder.seqR[T].read(x).right.map(_.toList)
    }
  implicit lazy val AddGlobalImportReader: ConfDecoder[AddGlobalImport] =
    importerReader.map(AddGlobalImport.apply)
  implicit lazy val RemoveGlobalImportReader: ConfDecoder[RemoveGlobalImport] =
    importerReader.map(RemoveGlobalImport.apply)
}
