package scala.meta.internal.pc

import scala.{meta => m}
import scala.collection.mutable
import scala.tools.nsc.Settings
import scala.tools.nsc.interactive.ScalafixGlobalProxy
import scala.tools.nsc.interactive.Global
import scala.meta.io.AbsolutePath
import scala.reflect.io.VirtualDirectory
import java.io.File
import scala.tools.nsc.reporters.StoreReporter
import java.{util => ju}
import scala.reflect.internal.{Flags => gf}
import scala.meta.internal.semanticdb.scalac.SemanticdbOps
import scala.util.control.NonFatal
import scala.collection.compat._ // Used for cross-compilation.

object ScalafixGlobal {
  def newCompiler(
      cp: List[AbsolutePath],
      options: List[String],
      symbolReplacements: Map[String, String]
  ): ScalafixGlobal = {
    val classpath = cp.mkString(File.pathSeparator)
    val vd = new VirtualDirectory("(memory)", None)
    val settings = new Settings
    settings.Ymacroexpand.value = "discard"
    settings.outputDirs.setSingleOutput(vd)
    settings.classpath.value = classpath
    settings.YpresentationAnyThread.value = true
    if (classpath.isEmpty) {
      settings.usejavacp.value = true
    }
    val (isSuccess, unprocessed) =
      settings.processArguments(options, processAll = true)
    require(isSuccess, unprocessed)
    require(unprocessed.isEmpty, unprocessed)
    new ScalafixGlobal(settings, new StoreReporter(), symbolReplacements)
  }
}

class ScalafixGlobal(
    settings: Settings,
    val storeReporter: StoreReporter,
    symbolReplacements: Map[String, String]
) extends Global(settings, storeReporter)
    with ScalafixGlobalProxy { compiler =>
  hijackPresentationCompilerThread()

  lazy val gsymbolReplacements: Map[String, Symbol] = (for {
    (key, value) <- symbolReplacements.toSeq
    gkey <- inverseSemanticdbSymbols(key)
    if semanticdbSymbol(gkey) == key
    gvalue <- inverseSemanticdbSymbols(value)
    if semanticdbSymbol(gvalue) == value
  } yield (key, gvalue)).toMap

  override val shorthands: Set[String] = Set.empty

  def inverseSemanticdbSymbols(symbol: String): List[Symbol] = {
    import scala.meta.internal.semanticdb.Scala._
    if (!symbol.isGlobal) return Nil

    def loop(s: String): List[Symbol] = {
      if (s.isNone || s.isRootPackage) rootMirror.RootPackage :: Nil
      else if (s.isEmptyPackage) rootMirror.EmptyPackage :: Nil
      else if (s.isPackage) {
        try {
          rootMirror.staticPackage(s.stripSuffix("/").replace("/", ".")) :: Nil
        } catch {
          case NonFatal(_) =>
            Nil
        }
      } else {
        val (desc, parent) = DescriptorParser(s)
        val parentSymbol = loop(parent)

        def tryMember(sym: Symbol): List[Symbol] =
          sym match {
            case NoSymbol =>
              Nil
            case owner =>
              desc match {
                case Descriptor.None =>
                  Nil
                case Descriptor.Type(value) =>
                  val member = owner.info.decl(TypeName(value)) :: Nil
                  if (sym.isJava) owner.info.decl(TermName(value)) :: member
                  else member
                case Descriptor.Term(value) =>
                  owner.info.decl(TermName(value)) :: Nil
                case Descriptor.Package(value) =>
                  owner.info.decl(TermName(value)) :: Nil
                case Descriptor.Parameter(value) =>
                  owner.paramss.flatten.filter(_.name.containsName(value))
                case Descriptor.TypeParameter(value) =>
                  owner.typeParams.filter(_.name.containsName(value))
                case Descriptor.Method(value, _) =>
                  owner.info
                    .decl(TermName(value))
                    .alternatives
                    .iterator
                    .filter(sym => semanticdbSymbol(sym) == s)
                    .toList
              }
          }

        parentSymbol.flatMap(tryMember)
      }
    }

    try loop(symbol).filterNot(_ == NoSymbol)
    catch {
      case NonFatal(e) =>
        println(s"invalid SemanticDB symbol: $symbol\n${e.getMessage}")
        Nil
    }
  }

  class MetalsGlobalSemanticdbOps(val global: compiler.type)
      extends SemanticdbOps
  lazy val semanticdbOps = new MetalsGlobalSemanticdbOps(compiler)

  def semanticdbSymbol(symbol: Symbol): String = {
    import semanticdbOps._
    symbol.toSemantic
  }
  def inverseSemanticdbSymbol(sym: String): Symbol =
    inverseSemanticdbSymbols(sym).headOption.getOrElse(NoSymbol)

  def renamedSymbols(context: Context): collection.Map[Symbol, Name] = {
    val result = mutable.Map.empty[Symbol, Name]
    context.imports.foreach { imp =>
      lazy val pre = imp.qual.tpe
      imp.tree.selectors.foreach { sel =>
        if (sel.rename != null) {
          val member = pre.member(sel.name)
          result(member) = sel.rename
          member.companion match {
            case NoSymbol =>
            case companion =>
              result(companion) = sel.rename
          }
        }
      }
    }
    result
  }
  lazy val renameConfig: collection.Map[Symbol, Name] =
    Map[String, String](
      "scala/collection/mutable/" -> "mutable.",
      "java/util/" -> "ju."
    ).map {
        case (sym, name) =>
          val nme =
            if (name.endsWith("#")) TypeName(name.stripSuffix("#"))
            else if (name.endsWith(".")) TermName(name.stripSuffix("."))
            else TermName(name)
          inverseSemanticdbSymbol(sym) -> nme
      }
      .view
      .filterKeys(_ != NoSymbol)
      .toMap

  def shortType(longType: Type, history: ShortenedNames): Type = {
    val isVisited = mutable.Set.empty[(Type, Option[ShortName])]
    val cached = new ju.HashMap[(Type, Option[ShortName]), Type]()
    def loop(tpe: Type, name: Option[ShortName]): Type = {
      val key = tpe -> name
      // NOTE(olafur) Prevent infinite recursion, see https://github.com/scalameta/metals/issues/749
      if (isVisited(key)) return cached.getOrDefault(key, tpe)
      isVisited += key
      val result = tpe match {
        case TypeRef(pre, sym, args) =>
          if (history.isSymbolInScope(sym, pre)) {
            TypeRef(NoPrefix, sym, args.map(arg => loop(arg, None)))
          } else {
            val ownerSymbol = pre.termSymbol
            history.config.get(ownerSymbol) match {
              case Some(rename)
                  if history.tryShortenName(ShortName(rename, ownerSymbol)) =>
                TypeRef(
                  new PrettyType(rename.toString),
                  sym,
                  args.map(arg => loop(arg, None))
                )
              case _ =>
                history.renames.get(sym) match {
                  case Some(rename)
                      if history.nameResolvesToSymbol(rename, sym) =>
                    TypeRef(
                      NoPrefix,
                      sym.newErrorSymbol(rename),
                      args.map(arg => loop(arg, None))
                    )
                  case _ if history.isSymbolInScope(sym, pre) =>
                    TypeRef(
                      NoPrefix,
                      sym,
                      args.map(arg => loop(arg, None))
                    )
                  case _ =>
                    if (sym.isAliasType &&
                      (sym.isAbstract ||
                      sym.overrides.lastOption.exists(_.isAbstract))) {

                      // Always dealias abstract type aliases but leave concrete aliases alone.
                      // trait Generic { type Repr /* dealias */ }
                      // type Catcher[T] = PartialFunction[Throwable, T] // no dealias
                      loop(tpe.dealias, name)
                    } else if (history.owners(pre.typeSymbol)) {
                      if (history.nameResolvesToSymbol(sym.name, sym)) {
                        TypeRef(NoPrefix, sym, args.map(arg => loop(arg, None)))
                      } else {
                        TypeRef(
                          ThisType(pre.typeSymbol),
                          sym,
                          args.map(arg => loop(arg, None))
                        )
                      }
                    } else {
                      TypeRef(
                        loop(pre, Some(ShortName(sym))),
                        sym,
                        args.map(arg => loop(arg, None))
                      )
                    }
                }
            }
          }
        case SingleType(pre, sym) =>
          if (sym.isScalaPackageObject) {
            // NOTE(olafur): special-case scala package object because
            // `Type.toString()` doesn't print the "scala." prefix in
            // "scala.Seq[T]" even when it's needed.
            loop(ThisType(sym.owner), name)
          } else if (sym.hasPackageFlag || sym.isPackageObjectOrClass) {
            if (history.tryShortenName(name)) NoPrefix
            else tpe
          } else {
            if (history.isSymbolInScope(sym, pre)) SingleType(NoPrefix, sym)
            else {
              pre match {
                case ThisType(psym) if history.isSymbolInScope(psym, pre) =>
                  SingleType(NoPrefix, sym)
                case _ =>
                  SingleType(loop(pre, Some(ShortName(sym))), sym)
              }
            }
          }
        case ThisType(sym) =>
          if (history.tryShortenName(name)) NoPrefix
          else {
            val owners = sym.ownerChain
            val prefix = owners.indexWhere { owner =>
              owner.owner != definitions.ScalaPackageClass &&
              history.tryShortenName(
                Some(ShortName(owner.name, owner))
              )
            }
            if (prefix < 0) {
              new PrettyType(history.fullname(sym))
            } else {
              val names = owners
                .take(prefix + 1)
                .reverse
                .map(s => m.Term.Name(s.nameSyntax))
              val ref = names.tail.foldLeft(names.head: m.Term.Ref) {
                case (qual, name) => m.Term.Select(qual, name)
              }
              new PrettyType(ref.syntax)
            }
          }
        case ConstantType(Constant(sym: TermSymbol))
            if sym.hasFlag(gf.JAVA_ENUM) =>
          loop(SingleType(sym.owner.thisPrefix, sym), None)
        case ConstantType(Constant(tpe: Type)) =>
          ConstantType(Constant(loop(tpe, None)))
        case SuperType(thistpe, supertpe) =>
          SuperType(loop(thistpe, None), loop(supertpe, None))
        case RefinedType(parents, decls) =>
          RefinedType(parents.map(parent => loop(parent, None)), decls)
        case AnnotatedType(annotations, underlying) =>
          AnnotatedType(annotations, loop(underlying, None))
        case ExistentialType(quantified, underlying) =>
          ExistentialType(
            quantified.map(sym => sym.setInfo(loop(sym.info, None))),
            loop(underlying, None)
          )
        case PolyType(tparams, resultType) =>
          PolyType(tparams, resultType.map(t => loop(t, None)))
        case NullaryMethodType(resultType) =>
          loop(resultType, None)
        case TypeBounds(lo, hi) =>
          TypeBounds(loop(lo, None), loop(hi, None))
        case MethodType(params, resultType) =>
          MethodType(params, loop(resultType, None))
        case ErrorType =>
          definitions.AnyTpe
        case t => t
      }
      cached.putIfAbsent(key, result)
      result
    }

    longType match {
      case ThisType(_) => longType
      case _ => loop(longType, None)
    }
  }

  case class ShortName(
      name: Name,
      symbol: Symbol
  ) {
    def isRename: Boolean = symbol.name != name
    def asImport: String = {
      val ident = Identifier(name)
      if (isRename) s"${Identifier(symbol.name)} => ${ident}"
      else ident
    }
    def owner: Symbol = symbol.owner
  }
  object ShortName {
    def apply(sym: Symbol): ShortName =
      ShortName(sym.name, sym)
  }

  class ShortenedNames(
      val missingImports: mutable.Map[Name, ShortName] = mutable.Map.empty,
      val lookupSymbol: Name => List[NameLookup] = _ => Nil,
      val config: collection.Map[Symbol, Name] = Map.empty,
      val renames: collection.Map[Symbol, Name] = Map.empty,
      val owners: collection.Set[Symbol] = Set.empty
  ) {
    def this(context: Context) =
      this(lookupSymbol = { name =>
        context.lookupSymbol(name, _ => true) :: Nil
      })

    def fullname(sym: Symbol): String = {
      if (topSymbolResolves(sym)) sym.fullNameSyntax
      else s"_root_.${sym.fullNameSyntax}"
    }

    def topSymbolResolves(sym: Symbol): Boolean = {
      // Returns the package `a` for the symbol `_root_/a/b.c`
      def topPackage(s: Symbol): Symbol = {
        val owner = s.owner
        if (s.isRoot || s.isRootPackage || s == NoSymbol || s.owner.isEffectiveRoot || s == owner) {
          s
        } else {
          topPackage(owner)
        }
      }
      val top = topPackage(sym)
      nameResolvesToSymbol(top.name.toTermName, top)
    }

    def isSymbolInScope(sym: Symbol, prefix: Type = NoPrefix): Boolean = {
      nameResolvesToSymbol(sym.name, sym, prefix)
    }
    def nameResolvesToSymbol(
        name: Name,
        sym: Symbol,
        prefix: Type = NoPrefix
    ): Boolean = {
      lookupSymbol(name) match {
        case Nil => true
        case lookup =>
          lookup.exists {
            case LookupSucceeded(qual, symbol) =>
              symbol.isKindaTheSameAs(sym) && {
                prefix == NoPrefix ||
                prefix.isInstanceOf[PrettyType] ||
                qual.tpe.computeMemberType(symbol) <:<
                  prefix.computeMemberType(sym)
              }
            case l => l.symbol.isKindaTheSameAs(sym)
          }
      }
    }

    def tryShortenName(short: ShortName): Boolean = {
      val ShortName(name, sym) = short
      missingImports.get(name) match {
        case Some(ShortName(_, other)) =>
          other.isKindaTheSameAs(sym)
        case _ =>
          val results =
            Iterator(lookupSymbol(name), lookupSymbol(name.otherName))
          results.flatten.filter(_ != LookupNotFound).toList match {
            case Nil =>
              missingImports(name) = short
              true
            case lookup =>
              lookup.forall(_.symbol.isKindaTheSameAs(sym))
          }
      }
    }
    def tryShortenName(name: Option[ShortName]): Boolean =
      name match {
        case Some(short) =>
          tryShortenName(short)
        case _ =>
          false
      }

  }

  implicit class XtensionNameMetals(name: Name) {
    def otherName: Name =
      if (name.isTermName) name.toTypeName
      else name.toTermName
  }
  implicit class XtensionSymbolMetals(sym: Symbol) {
    def isScalaPackageObject: Boolean = {
      sym.isPackageObject &&
      sym.owner == definitions.ScalaPackageClass
    }
    def javaClassSymbol: Symbol = {
      if (sym.isJavaModule && !sym.hasPackageFlag) sym.companionClass
      else sym
    }
    def nameSyntax: String = {
      if (sym.isEmptyPackage || sym.isEmptyPackageClass) "_empty_"
      else if (sym.isRootPackage || sym.isRoot) "_root_"
      else sym.nameString
    }
    def fullNameSyntax: String = {
      val out = new java.lang.StringBuilder
      def loop(s: Symbol): Unit = {
        if (s.isRoot || s.isRootPackage || s == NoSymbol || s.owner.isEffectiveRoot) {
          out.append(Identifier(s.nameSyntax))
        } else {
          loop(s.effectiveOwner.enclClass)
          out.append('.').append(Identifier(s.name))
        }
      }
      loop(sym)
      out.toString
    }
    def isLocallyDefinedSymbol: Boolean = {
      sym.isLocalToBlock && sym.pos.isDefined
    }

    def asInfixPattern: Option[String] =
      if (sym.isCase &&
        !Character.isUnicodeIdentifierStart(sym.decodedName.head)) {
        sym.primaryConstructor.paramss match {
          case (a :: b :: Nil) :: _ =>
            Some(s"${a.decodedName} ${sym.decodedName} ${b.decodedName}")
          case _ => None
        }
      } else {
        None
      }

    def isKindaTheSameAs(other: Symbol): Boolean = {
      if (other == NoSymbol) sym == NoSymbol
      else if (sym == NoSymbol) false
      else if (sym.hasPackageFlag) {
        // NOTE(olafur) hacky workaround for comparing module symbol with package symbol
        other.fullName == sym.fullName
      } else {
        other.dealiased == sym.dealiased ||
        other.companion == sym.dealiased ||
        semanticdbSymbol(other.dealiased) == semanticdbSymbol(sym.dealiased)
      }
    }

    def snippetCursor: String = sym.paramss match {
      case Nil =>
        "$0"
      case Nil :: Nil =>
        "()$0"
      case _ =>
        "($0)"
    }

    def isDefined: Boolean =
      sym != null &&
        sym != NoSymbol &&
        !sym.isErroneous

    def isNonNullaryMethod: Boolean =
      sym.isMethod &&
        !sym.info.isInstanceOf[NullaryMethodType] &&
        !sym.paramss.isEmpty

    def isJavaModule: Boolean =
      sym.isJava && sym.isModule

    def hasTypeParams: Boolean =
      sym.typeParams.nonEmpty ||
        (sym.isJavaModule && sym.companionClass.typeParams.nonEmpty)

    def requiresTemplateCurlyBraces: Boolean = {
      sym.isTrait || sym.isInterface || sym.isAbstractClass
    }
    def isTypeSymbol: Boolean =
      sym.isType ||
        sym.isClass ||
        sym.isTrait ||
        sym.isInterface ||
        sym.isJavaModule

    def dealiasedSingleType: Symbol =
      if (sym.isValue) {
        sym.info.resultType match {
          case SingleType(_, dealias) => dealias
          case _ => sym
        }
      } else {
        sym
      }
    def dealiased: Symbol =
      if (sym.isAliasType) sym.info.dealias.typeSymbol
      else if (sym.isValue) dealiasedSingleType
      else sym
  }

  /**
   * A `Type` with custom pretty-printing representation, not used for typechecking.
   *
   * NOTE(olafur) Creating a new `Type` subclass is a hack, a better long-term solution would be
   * to implement a custom pretty-printer for types so that we don't have to rely on `Type.toString`.
   */
  class PrettyType(
      override val prefixString: String,
      override val safeToString: String
  ) extends Type {
    def this(string: String) =
      this(string + ".", string)
  }

}
