package scalafix
package internal.config

import scala.meta.Ref
import scala.meta._
import scala.meta.parsers.Parse
import scala.meta.semanticdb.Symbol
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
import scalafix.internal.config.MetaconfigParser.{parser => hoconParser}
import scalafix.patch.TreePatch
import scalafix.rewrite.ConfigRewrite

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

  object UriRewriteString {
    def unapply(arg: Conf.Str): Option[(String, String)] =
      UriRewrite.unapply(arg).map {
        case (a, b) => a -> b.getSchemeSpecificPart
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
        val combinedRewrites: Conf.Lst =
          if (extraRewrites.nonEmpty)
            Conf.Lst(extraRewrites.map(Conf.Str))
          else
            rewriteConf match {
              case rewrites @ Conf.Lst(_) => rewrites
              case x => Conf.Lst(x :: Nil)
            }
        rewriteDecoder.read(combinedRewrites).map(rewrite => rewrite -> config)
    }

  def defaultRewriteDecoder(getSemanticCtx: LazySemanticCtx): ConfDecoder[Rewrite] =
    ConfDecoder.instance[Rewrite] {
      case conf @ Conf.Str(value) =>
        val isSyntactic = ScalafixRewrites.syntacticNames.contains(value)
        val kind = RewriteKind(syntactic = isSyntactic)
        val semanticCtx = getSemanticCtx(kind)
        val names: Map[String, Rewrite] =
          ScalafixRewrites.syntaxName2rewrite ++
            semanticCtx.fold(Map.empty[String, Rewrite])(
              ScalafixRewrites.name2rewrite)
        ReaderUtil.fromMap(names).read(conf)
    }

  private lazy val semanticRewriteClass = classOf[SemanticRewrite]

  def classloadRewrite(semanticCtx: LazySemanticCtx): Class[_] => Seq[SemanticCtx] = {
    cls =>
      val semanticRewrite =
        cls.getClassLoader.loadClass("scalafix.rewrite.SemanticRewrite")
      val kind =
        if (semanticRewriteClass.isAssignableFrom(cls)) RewriteKind.Semantic
        else RewriteKind.Syntactic
      semanticCtx(kind).toList
  }

  private lazy val SlashSeparated = "([^/]+)/(.*)".r

  private def requireSemanticSemanticCtx[T](semanticCtx: LazySemanticCtx, what: String)(
      f: SemanticCtx => Configured[T]): Configured[T] = {
    semanticCtx(RewriteKind.Semantic).fold(
      Configured.error(s"$what requires the semantic API."): Configured[T])(f)
  }

  def parseReplaceSymbol(
      from: String,
      to: String): Configured[(Symbol.Global, Symbol.Global)] =
    symbolGlobalReader.read(Conf.Str(from)) |@|
      symbolGlobalReader.read(Conf.Str(to))

  def classloadRewriteDecoder(semanticCtx: LazySemanticCtx): ConfDecoder[Rewrite] =
    ConfDecoder.instance[Rewrite] {
      case UriRewriteString("scala", fqn) =>
        ClassloadRewrite(fqn, classloadRewrite(semanticCtx))
      case UriRewriteString("replace", replace @ SlashSeparated(from, to)) =>
        requireSemanticSemanticCtx(semanticCtx, replace) { m =>
          parseReplaceSymbol(from, to)
            .map(TreePatch.ReplaceSymbol.tupled)
            .map(p => Rewrite.constant(replace, p, m))
        }
    }

  def baseSyntacticRewriteDecoder: ConfDecoder[Rewrite] =
    baseRewriteDecoders(_ => None)
  def baseRewriteDecoders(semanticCtx: LazySemanticCtx): ConfDecoder[Rewrite] = {
    MetaconfigPendingUpstream.orElse(
      defaultRewriteDecoder(semanticCtx),
      classloadRewriteDecoder(semanticCtx)
    )
  }
  def configFromInput(
      input: Input,
      semanticCtx: LazySemanticCtx,
      extraRewrites: List[String])(
      implicit decoder: ConfDecoder[Rewrite]
  ): Configured[(Rewrite, ScalafixConfig)] = {
    hoconParser.fromInput(input).andThen { conf =>
      scalafixConfigConfDecoder(decoder, extraRewrites)
        .read(conf)
        .andThen {
          case (rewrite, config) =>
            ConfigRewrite(config.patches, semanticCtx).map { configRewrite =>
              configRewrite.fold(rewrite -> config)(
                rewrite.andThen(_) -> config)
            }
        }
    }
  }

  implicit lazy val ReplaceSymbolReader: ConfDecoder[ReplaceSymbol] =
    ConfDecoder.instanceF[ReplaceSymbol] { c =>
      (
        c.get[Symbol.Global]("from") |@|
          c.get[Symbol.Global]("to")
      ).map { case (a, b) => ReplaceSymbol(a, b) }
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
      case els =>
        fallbackFilterMatcher.reader.read(els)
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
  implicit lazy val symbolReader: ConfDecoder[Symbol] =
    ConfDecoder.stringConfDecoder.map(Symbol.apply)
  private def parseSymbol(sym: String): Configured[Symbol] =
    try Ok(Symbol(sym)) // Because https://github.com/scalameta/scalameta/issues/821
    catch { case NonFatal(e) => ConfError.exception(e, 0).notOk }
  implicit lazy val symbolGlobalReader: ConfDecoder[Symbol.Global] =
    ConfDecoder.instance[Symbol.Global] {
      case Conf.Str(path) =>
        def symbolGlobal(symbol: Symbol): Configured[Symbol.Global] =
          symbol match {
            case g: Symbol.Global => Ok(g)
            case els =>
              ConfError
                .typeMismatch(
                  "Symbol.Global",
                  Conf.Str(s"$els: ${els.productPrefix}"))
                .notOk
          }
        val toParse =
          if (path.startsWith("_")) path
          else s"_root_.$path."
        parseSymbol(toParse).andThen(symbolGlobal)
    }
  implicit lazy val AddGlobalImportReader: ConfDecoder[AddGlobalImport] =
    importerReader.map(AddGlobalImport.apply)
  implicit lazy val RemoveGlobalImportReader: ConfDecoder[RemoveGlobalImport] =
    termRefReader.flatMap { ref =>
      parseSymbol(s"_root_.$ref.").map { s =>
        RemoveGlobalImport(s)
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
