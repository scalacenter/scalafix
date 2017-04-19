package scalafix
package config

import scala.meta.Ref
import scala.meta._
import scala.meta.parsers.Parse
import scala.meta.semantic.v1.Symbol
import scala.reflect.ClassTag
import scala.util.Try
import scala.util.matching.Regex
import scalafix.rewrite.ScalafixMirror
import scalafix.rewrite.ScalafixRewrites
import scalafix.util.ClassloadRewrite
import scalafix.util.FileOps
import scalafix.patch.TreePatch._

import java.io.File
import java.io.OutputStream
import java.io.PrintStream
import java.net.URI
import java.net.URL

import metaconfig.Conf
import metaconfig.ConfDecoder
import metaconfig.ConfError
import metaconfig.Configured
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

  object FromClassloadRewrite {
    def unapply(arg: Conf.Str): Option[String] = arg match {
      case UriRewrite("scala", uri) =>
        Option(uri.getSchemeSpecificPart)
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

  def scalafixConfigEmptyRewriteReader: ConfDecoder[(Conf, ScalafixConfig)] =
    ConfDecoder.instance[(Conf, ScalafixConfig)] {
      case Conf.Obj(values) =>
        val (rewrites, noRewrites) = values.partition(_._1 == "rewrites")
        val rewriteConf =
          Configured.Ok(rewrites.lastOption.map(_._2).getOrElse(Conf.Lst()))
        val config =
          ScalafixConfig.syntaxConfDecoder.read(Conf.Obj(noRewrites))
        rewriteConf.product(config)
    }
  def scalafixConfigConfDecoder(mirror: Option[ScalafixMirror])(
      implicit rewriteDecoder: ConfDecoder[Rewrite]
  ): ConfDecoder[ScalafixConfig] = {
    mirror match {
      case None => scalafixConfigEmptyRewriteReader.map(_._2)
      case Some(mirror) =>
        scalafixConfigEmptyRewriteReader.flatMap {
          case (rewriteConf, scalafixConfig) =>
            implicit val rewritesReader =
              implicitly[ConfDecoder[List[Rewrite]]]
            rewritesReader
              .read(rewriteConf)
              .map { rewrites =>
                val combined =
                  rewrites.foldLeft(Rewrite.emptySemantic(mirror))(_ andThen _)
                scalafixConfig.copy(rewrite = combined)
              }
        }
    }
  }

  def rewriteConfDecoder(
      mirror: Option[ScalafixMirror]): ConfDecoder[Rewrite] =
    ConfDecoder.instance[Rewrite] {
      case FromClassloadRewrite(fqn) =>
        ClassloadRewrite(fqn, mirror.toList)
      case els =>
        val name2rewrite = mirror match {
          case Some(mirror) => ScalafixRewrites.name2rewrite(mirror)
          case None => ScalafixRewrites.syntaxName2rewrite
        }
        ReaderUtil.fromMap(name2rewrite).read(els)
    }

  object ConfStrLst {
    def unapply(arg: Conf.Lst): Option[List[String]] =
      if (arg.values.forall(_.isInstanceOf[Conf.Str]))
        Some(arg.values.collect { case Conf.Str(value) => value })
      else None
  }

  implicit val RegexReader: ConfDecoder[Regex] = ConfDecoder.instance[Regex] {
    case Conf.Str(str) => Configured.Ok(FilterMatcher.mkRegexp(List(str)))
    case ConfStrLst(values) => Configured.Ok(FilterMatcher.mkRegexp(values))
  }
  private val fallbackFilterMatcher = FilterMatcher(Nil, Nil)
  implicit val FilterMatcherReader: ConfDecoder[FilterMatcher] =
    ConfDecoder.instance[FilterMatcher] {
      case Conf.Str(str) => Configured.Ok(FilterMatcher(str))
      case ConfStrLst(values) =>
        Configured.Ok(FilterMatcher(values, Nil))
      case els => fallbackFilterMatcher.reader.read(els)
    }

  def parseReader[T](implicit parse: Parse[T]): ConfDecoder[T] =
    ConfDecoder.stringConfDecoder.flatMap { str =>
      str.parse[T] match {
        case parsers.Parsed.Success(x) => Configured.Ok(x)
        case parsers.Parsed.Error(pos, msg, _) =>
          ConfError.parseError(pos, msg).notOk
      }
    }

  def castReader[From, To](ConfDecoder: ConfDecoder[From])(
      implicit ev: ClassTag[To]): ConfDecoder[To] = ConfDecoder.flatMap {
    case x if ev.runtimeClass.isInstance(x) =>
      Configured.Ok(x.asInstanceOf[To])
    case x =>
      ConfError.msg(s"Expected Ref, got ${x.getClass}").notOk
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
    ConfDecoder.stringConfDecoder.map(Symbol.apply)
  implicit lazy val AddGlobalImportReader: ConfDecoder[AddGlobalImport] =
    importerReader.map(AddGlobalImport.apply)
  implicit lazy val RemoveGlobalImportReader: ConfDecoder[RemoveGlobalImport] =
    importerReader.map(RemoveGlobalImport.apply)

  implicit lazy val PrintStreamReader: ConfDecoder[PrintStream] = {
    val empty = new PrintStream(new OutputStream {
      override def write(b: Int): Unit = ()
    })
    ReaderUtil.oneOf[PrintStream](empty)
  }
}
