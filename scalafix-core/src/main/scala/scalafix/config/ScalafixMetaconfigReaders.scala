package scalafix.config

import scala.meta._
import scala.meta.Ref
import scala.meta.parsers.Parse
import scala.meta.semantic.v1.Symbol
import scala.reflect.ClassTag
import scala.util.Try
import scala.util.matching.Regex
import scalafix.Failure.UnknownRewrite
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

import metaconfig.Reader
import org.scalameta.logger

object ScalafixMetaconfigReaders extends ScalafixMetaconfigReaders
// A collection of metaconfig.Reader instances that are shared across
trait ScalafixMetaconfigReaders {
  implicit lazy val parseReader: Reader[MetaParser] = {
    import scala.meta.parsers.Parse._
    ReaderUtil.oneOf[MetaParser](parseSource, parseStat, parseCase)
  }
  implicit lazy val dialectReader: Reader[Dialect] = {
    import scala.meta.dialects._
    ReaderUtil.oneOf[Dialect](Scala211, Sbt0137, Dotty, Paradise211)
  }

  object ClassloadRewrite {
    def unapply(arg: String): Option[String] = arg match {
      case UriRewrite("scala", uri) =>
        val suffix = if (arg.endsWith("$")) "" else "$"
        Option(uri.getSchemeSpecificPart + suffix)
      case _ => None
    }
  }

  object UriRewrite {
    def unapply(arg: String): Option[(String, URI)] =
      for {
        uri <- Try(new URI(arg)).toOption
        scheme <- Option(uri.getScheme)
      } yield scheme -> uri
  }

  object UrlRewrite {
    def unapply(arg: String): Option[URL] = arg match {
      case UriRewrite("http" | "https", uri) if uri.isAbsolute =>
        Option(uri.toURL)
      case _ => None
    }
  }

  object FileRewrite {
    def unapply(arg: String): Option[File] = arg match {
      case UriRewrite("file", uri) =>
        Option(new File(uri.getSchemeSpecificPart).getAbsoluteFile)
      case _ => None
    }
  }

  object FromSourceRewrite {
    def unapply(arg: String): Option[String] = arg match {
      case FileRewrite(file) => Option(FileOps.readFile(file))
      case UrlRewrite(url) => Option(FileOps.readURL(url))
      case _ => None
    }
  }

  implicit lazy val rewriteReader: Reader[ScalafixRewrite] =
    Reader.instance[ScalafixRewrite] {
      case ClassloadRewrite(fqn) =>
        ClassloadObject[ScalafixRewrite](fqn)
      case FromSourceRewrite(code) =>
        ScalafixToolbox.getRewrite[ScalafixMirror](code)
      case els =>
        ReaderUtil.fromMap(ScalafixRewrites.name2rewrite).read(els)
    }

  implicit val RegexReader: Reader[Regex] = Reader.instance[Regex] {
    case str: String => Right(FilterMatcher.mkRegexp(List(str)))
    case str: Seq[_] =>
      Right(FilterMatcher.mkRegexp(str.asInstanceOf[Seq[String]]))
  }
  private val fallbackFilterMatcher = FilterMatcher(Nil, Nil)
  implicit val FilterMatcherReader: Reader[FilterMatcher] =
    Reader.instance[FilterMatcher] {
      case str: String => Right(FilterMatcher(str))
      case str: Seq[_] =>
        Right(FilterMatcher(str.asInstanceOf[Seq[String]], Nil))
      case els => fallbackFilterMatcher.reader.read(els)
    }

  def parseReader[T](implicit parse: Parse[T]): Reader[T] =
    Reader.stringR.flatMap { str =>
      str.parse[T] match {
        case parsers.Parsed.Success(x) => Right(x)
        case parsers.Parsed.Error(_, x, _) =>
          Left(new IllegalArgumentException(x))
      }
    }

  def castReader[From, To](reader: Reader[From])(
      implicit ev: ClassTag[To]): Reader[To] = reader.flatMap {
    case x if ev.runtimeClass.isInstance(x) => Right(x.asInstanceOf[To])
    case x =>
      Left(new IllegalArgumentException(s"Expected Ref, got ${x.getClass}"))
  }
  implicit lazy val importerReader: Reader[Importer] = parseReader[Importer]
  implicit lazy val refReader: Reader[Ref] =
    castReader[Stat, Ref](parseReader[Stat])
  implicit lazy val termRefReader: Reader[Term.Ref] =
    castReader[Stat, Term.Ref](parseReader[Stat])
  protected[scalafix] val fallbackReplace = Replace(Symbol.None, q"IGNOREME")
  implicit lazy val replaceReader: Reader[Replace] = fallbackReplace.reader
  implicit lazy val symbolReader: Reader[Symbol] =
    Reader.stringR.map(Symbol.apply)
  implicit def listReader[T: Reader]: Reader[List[T]] =
    Reader.seqR[T].map(_.toList)
  implicit lazy val AddGlobalImportReader: Reader[AddGlobalImport] =
    importerReader.map(AddGlobalImport.apply)
  implicit lazy val RemoveGlobalImportReader: Reader[RemoveGlobalImport] =
    importerReader.map(RemoveGlobalImport.apply)
}
