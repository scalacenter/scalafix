package scalafix
package config

import scala.meta.Ref
import scala.meta._
import scala.meta.parsers.Parse
import scala.meta.semantic.Symbol
import scala.reflect.ClassTag
import scala.util.Try
import scala.util.matching.Regex
import scalafix.patch.TreePatch._
import scalafix.rewrite.ScalafixRewrites
import scalafix.internal.util.ClassloadRewrite
import java.io.OutputStream
import java.io.PrintStream
import java.net.URI
import java.util.regex.Pattern
import scala.collection.immutable.Seq
import scala.util.control.NonFatal
import metaconfig.Conf
import metaconfig.ConfDecoder
import metaconfig.ConfError
import metaconfig.Configured
import metaconfig.Configured.Ok
import scalafix.config.MetaconfigParser.{parser => hoconParser}

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

  private val rewriteReges = Pattern.compile("rewrites?")
  private def isRewriteKey(key: (String, Conf)) =
    rewriteReges.matcher(key._1).matches()
  def scalafixConfigEmptyRewriteReader: ConfDecoder[(Conf, ScalafixConfig)] =
    ConfDecoder.instance[(Conf, ScalafixConfig)] {
      case Conf.Obj(values) =>
        val (rewrites, noRewrites) = values.partition(isRewriteKey)
        val rewriteConf =
          Configured.Ok(rewrites.lastOption.map(_._2).getOrElse(Conf.Lst()))
        val config =
          ScalafixConfig.ScalafixConfigDecoder.read(Conf.Obj(noRewrites))
        rewriteConf.product(config)
    }
  def scalafixConfigConfDecoder(
      rewriteDecoder: ConfDecoder[Rewrite],
      extraRewrites: List[String] = Nil
  ): ConfDecoder[(Rewrite, ScalafixConfig)] =
    scalafixConfigEmptyRewriteReader.flatMap {
      case (rewriteConf, config) =>
        val rewriteList: Seq[Conf] = rewriteConf match {
          case Conf.Lst(rewrites) => rewrites
          case x => x :: Nil
        }
        val combinedRewrites =
          Conf.Lst(extraRewrites.map(Conf.Str) ++ rewriteList)
        rewriteDecoder.read(combinedRewrites).map(rewrite => rewrite -> config)
    }

  def defaultRewriteDecoder(getMirror: LazyMirror): ConfDecoder[Rewrite] =
    ConfDecoder.instance[Rewrite] {
      case conf @ Conf.Str(value) =>
        val isSyntactic = ScalafixRewrites.syntacticNames.contains(value)
        val kind = RewriteKind(syntactic = isSyntactic)
        val mirror = getMirror(kind)
        val names: Map[String, Rewrite] =
          ScalafixRewrites.syntaxName2rewrite ++
            mirror.fold(Map.empty[String, Rewrite])(
              ScalafixRewrites.name2rewrite)
        ReaderUtil.fromMap(names).read(conf)
    }

  private lazy val semanticRewriteClass = classOf[SemanticRewrite]

  def classloadRewrite(mirror: LazyMirror): Class[_] => Seq[Mirror] = { cls =>
    val kind =
      if (cls.isAssignableFrom(semanticRewriteClass)) RewriteKind.Semantic
      else RewriteKind.Syntactic
    mirror(kind).toList
  }

  def classloadRewriteDecoder(mirror: LazyMirror): ConfDecoder[Rewrite] =
    ConfDecoder.instance[Rewrite] {
      case FromClassloadRewrite(fqn) =>
        ClassloadRewrite(fqn, classloadRewrite(mirror))
    }

  def baseSyntacticRewriteDecoder: ConfDecoder[Rewrite] =
    baseRewriteDecoders(_ => None)
  def baseRewriteDecoders(mirror: LazyMirror): ConfDecoder[Rewrite] = {
    MetaconfigPendingUpstream.orElse(
      defaultRewriteDecoder(mirror),
      classloadRewriteDecoder(mirror)
    )
  }
  def configFromInput(input: Input,
                      mirror: LazyMirror,
                      extraRewrites: List[String])(
      implicit decoder: ConfDecoder[Rewrite]
  ): Configured[(Rewrite, ScalafixConfig)] =
    for {
      conf <- hoconParser.fromInput(input)
      rewriteAndConfig <- config
        .scalafixConfigConfDecoder(decoder, extraRewrites)
        .read(conf)
      (rewrite, config) = rewriteAndConfig
      patchRewrite <- Rewrite.patchRewrite(config.patches, mirror)
    } yield {
      patchRewrite.fold(rewrite -> config)(rewrite.andThen(_) -> config)
    }

  def rewriteConfDecoderSyntactic(
      singleRewriteDecoder: ConfDecoder[Rewrite]): ConfDecoder[Rewrite] =
    rewriteConfDecoder(singleRewriteDecoder)
  def rewriteConfDecoder(
      singleRewriteDecoder: ConfDecoder[Rewrite]): ConfDecoder[Rewrite] = {
    ConfDecoder.instance[Rewrite] {
      case Conf.Lst(values) =>
        MetaconfigPendingUpstream
          .flipSeq(values.map(singleRewriteDecoder.read))
          .map(rewrites => Rewrite.combine(rewrites))
      case rewrite @ Conf.Str(_) => singleRewriteDecoder.read(rewrite)
    }
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
  implicit lazy val importeeReader: ConfDecoder[Importee] =
    parseReader[Importee]
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
    termRefReader.flatMap { ref =>
      try {
        Ok(RemoveGlobalImport(Symbol(s"_root_.$ref.")))
      } catch {
        case NonFatal(e) =>
          ConfError.exception(e, 0).notOk
      }
    }

  implicit val metaconfigConfDecoder: ConfDecoder[Conf] =
    new ConfDecoder[Conf] {
      override def read(conf: Conf): Configured[Conf] = Ok(conf)
    }

  implicit lazy val PrintStreamReader: ConfDecoder[PrintStream] = {
    val empty = new PrintStream(new OutputStream {
      override def write(b: Int): Unit = ()
    })
    ReaderUtil.oneOf[PrintStream](empty)
  }
}
