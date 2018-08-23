package scalafix.internal.util

import scala.collection.mutable
import scala.meta._
import scala.meta.internal.symtab.SymbolTable
import scala.meta.internal.semanticdb.Scala._
import scala.meta.internal.semanticdb.SymbolInformation.{Kind => k}
import scala.meta.internal.semanticdb.SymbolInformation.{Property => p}
import scala.meta.internal.{semanticdb => s}
import scala.util.control.NoStackTrace
import scala.util.control.NonFatal
import scalapb.GeneratedMessage

case class PrettyResult[T <: Tree](tree: T, imports: List[String])

object PrettyType {

  def toTree(
      info: s.SymbolInformation,
      table: SymbolTable,
      shorten: QualifyStrategy,
      fatalErrors: Boolean
  ): PrettyResult[Tree] = {
    val pretty = unsafeInstance(table, shorten, fatalErrors)
    val result = pretty.toTree(info)
    PrettyResult(result, pretty.getImports())
  }

  def toType(
      tpe: s.Type,
      table: SymbolTable,
      shorten: QualifyStrategy,
      fatalErrors: Boolean
  ): PrettyResult[Type] = {
    val pretty = unsafeInstance(table, shorten, fatalErrors)
    val result = pretty.toType(tpe)
    PrettyResult(result, pretty.getImports())
  }

  def unsafeInstance(
      table: SymbolTable,
      shorten: QualifyStrategy,
      fatalErrors: Boolean
  ): PrettyType =
    new PrettyType(table, shorten, fatalErrors)

}

class PrettyType private (
    table: SymbolTable,
    shorten: QualifyStrategy,
    fatalErrors: Boolean
) {

  // TODO: workaround for https://github.com/scalameta/scalameta/issues/1492
  private val isCaseClassMethod = Set(
    "copy",
    "productPrefix",
    "productArity",
    "productElement",
    "productIterator",
    "canEqual",
    "hashCode",
    "toString",
    "equals"
  )
  private[this] val imports = List.newBuilder[String]

  def getImports(): List[String] = {
    val result = imports.result()
    imports.clear()
    result
  }

  private implicit class XtensionSymbolInformationProperties(
      info: s.SymbolInformation
  ) {
    def owner: String = info.symbol.owner
    def valueType: s.Type = {
      info.signature match {
        case s.ValueSignature(tpe) =>
          tpe
        case _ =>
          throw new IllegalArgumentException(
            s"Expected ValueSignature. Obtained: ${info.toString}"
          )
      }
    }
    def is(property: s.SymbolInformation.Property): Boolean =
      (info.properties & property.value) != 0
    def isVal: Boolean = is(p.VAL)
    def isVar: Boolean = is(p.VAR)
    def isVarSetter: Boolean =
      isVar && info.displayName.endsWith("_=")
  }
  private implicit class XtensionSymbolInfo(sym: String) {
    def toIndeterminateName: Name = Name(info(sym).displayName)
    def toTermName: Term.Name = Term.Name(info(sym).displayName)
    def toTypeName: Type.Name = Type.Name(info(sym).displayName)
  }

  private implicit class XtensionIterator(syms: Iterator[String]) {
    def scollect[T](f: PartialFunction[s.SymbolInformation, T]): List[T] =
      syms.map(info).collect(f).toList
    def sflatcollect[T](
        f: PartialFunction[s.SymbolInformation, Iterable[T]]
    ): List[T] =
      syms.map(info).collect(f).flatten.toList
    def smap[T](f: s.SymbolInformation => T): List[T] =
      syms.map(info).map(f).toList
  }
  private implicit class XtensionScopeHardlinks(scope: s.Scope) {
    def infos: List[s.SymbolInformation] =
      if (scope.hardlinks.isEmpty) scope.symlinks.iterator.map(info).toList
      else scope.hardlinks.toList
    def smap[T](f: s.SymbolInformation => T): List[T] =
      if (scope.hardlinks.isEmpty) scope.symlinks.smap(f)
      else scope.hardlinks.iterator.map(f).toList
  }
  private implicit class XtensionSymbols(syms: Seq[String]) {
    def scollect[T](f: PartialFunction[s.SymbolInformation, T]): List[T] =
      syms.iterator.map(info).collect(f).toList
    def smap[T](f: s.SymbolInformation => T): List[T] =
      syms.iterator.map(info).map(f).toList
  }

  def ref(sym: String): s.Type = {
    s.TypeRef(symbol = sym)
  }

  private implicit class XtensionSchemaType(tpe: s.Type) {
    def widen: s.Type = {
      tpe match {
        case s.SingleType(_, symbol) =>
          info(symbol).valueType
        case s.ConstantType(constant) =>
          constant match {
            case _: s.UnitConstant => ref("scala.Unit#")
            case _: s.BooleanConstant => ref("scala.Boolean#")
            case _: s.ByteConstant => ref("scala.Byte#")
            case _: s.ShortConstant => ref("scala.Short#")
            case _: s.CharConstant => ref("scala.Char#")
            case _: s.IntConstant => ref("scala.Int#")
            case _: s.LongConstant => ref("scala.Long#")
            case _: s.FloatConstant => ref("scala.Float#")
            case _: s.DoubleConstant => ref("scala.Double#")
            case _: s.StringConstant => ref("java.lang.String#")
            case _: s.NullConstant => ref("scala.Null#")
            case s.NoConstant => tpe
          }
        case _ =>
          tpe
      }
    }
  }

  private def info(sym: String): s.SymbolInformation = {
    table
      .info(sym)
      .orElse(hardlinks.get(sym))
      .getOrElse(throw new NoSuchElementException(sym))
  }

  def toTree(info: s.SymbolInformation): Tree = info.kind match {
    // Workaround for https://github.com/scalameta/scalameta/issues/1494
    case k.METHOD | k.FIELD if info.signature.isEmpty =>
      // Dummy value
      Defn.Val(
        Nil,
        Pat.Var(Term.Name(info.displayName)) :: Nil,
        None,
        q"???"
      )
    case k.FIELD =>
      if (info.is(p.FINAL)) {
        Decl.Val(
          toMods(info),
          Pat.Var(Term.Name(info.displayName)) :: Nil,
          toType(info.valueType)
        )
      } else {
        Decl.Var(
          toMods(info),
          Pat.Var(Term.Name(info.displayName)) :: Nil,
          toType(info.valueType)
        )
      }
    case k.PACKAGE =>
      toTermRef(info)
    case _ =>
      info.signature match {
        case s.MethodSignature(Some(tparams), paramss, returnType) =>
          val ret = unwrapRepeatedType(returnType)
          if (info.isVal) {
            Decl.Val(
              toMods(info),
              Pat.Var(Term.Name(info.displayName)) :: Nil,
              toType(ret)
            )
          } else if (info.isVar && !info.isVarSetter) {
            Decl.Var(
              toMods(info),
              Pat.Var(Term.Name(info.displayName)) :: Nil,
              toType(ret)
            )
          } else if (info.kind.isConstructor) {
            Ctor.Primary(
              toMods(info),
              Name(""),
              paramss.iterator
                .map(params => params.symbols.smap(toTermParam))
                .toList
            )
          } else {
            Decl.Def(
              toMods(info),
              Term.Name(info.displayName),
              tparams.smap(toTypeParam),
              paramss.iterator
                .map(params => params.symbols.smap(toTermParam))
                .toList,
              toType(ret)
            )
          }
        case s.ClassSignature(Some(tparams), parents, self, Some(decls)) =>
          val declarations = decls.infos
          val isCaseClass = info.is(p.CASE)
          def objectDecls: List[Stat] =
            declarations.flatMap {
              case i if !i.kind.isConstructor && !i.isVarSetter =>
                toStat(i)
              case _ =>
                Nil
            }

          def inits: List[Init] =
            parents.iterator
              .filterNot {
                case TypeExtractors.AnyRef() | TypeExtractors.Any() => true
                case parent =>
                  if (isCaseClass) {
                    parent match {
                      case TypeExtractors.Product() |
                          TypeExtractors.Serializable() =>
                        true
                      case _ =>
                        false
                    }
                  } else {
                    false
                  }
              }
              .map(toInit)
              .toList

          info.kind match {
            case k.TRAIT | k.INTERFACE =>
              Defn.Trait(
                toMods(info),
                Type.Name(info.displayName),
                tparams.smap(toTypeParam),
                Ctor.Primary(Nil, Name(""), Nil),
                Template(
                  Nil,
                  inits,
                  Self(Name(""), None),
                  declarations.flatMap { i =>
                    if (!i.kind.isConstructor &&
                      !i.isVarSetter) toStat(i)
                    else Nil
                  }
                )
              )
            case k.OBJECT =>
              Defn.Object(
                toMods(info),
                Term.Name(info.displayName),
                Template(
                  Nil,
                  inits,
                  Self(Name(""), None),
                  objectDecls
                )
              )
            case k.PACKAGE_OBJECT =>
              Pkg.Object(
                toMods(info),
                Term.Name(info.displayName),
                Template(
                  Nil,
                  inits,
                  Self(Name(""), None),
                  objectDecls
                )
              )
            case k.CLASS =>
              val ctor: Ctor.Primary = declarations
                .collectFirst {
                  case i if i.kind.isConstructor && i.is(p.PRIMARY) =>
                    toTree(i) match {
                      case ctor @ Ctor.Primary(_, _, Nil :: Nil)
                          if !info.is(p.CASE) =>
                        // Remove redudant () for non-case classes: class Foo
                        ctor.copy(paramss = Nil)
                      case e: Ctor.Primary => e
                    }
                }
                .getOrElse {
                  Ctor.Primary(Nil, Name(""), Nil)
                }

              // FIXME: Workaround for https://github.com/scalameta/scalameta/issues/1492
              val isCtorName = ctor.paramss.flatMap(_.map(_.name.value)).toSet
              def isSyntheticMember(m: s.SymbolInformation): Boolean =
                (isCaseClass && isCaseClassMethod(m.displayName)) ||
                  isCtorName(m.displayName)

              Defn.Class(
                toMods(info),
                Type.Name(info.displayName),
                tparams.smap(toTypeParam),
                ctor,
                Template(
                  Nil,
                  inits,
                  Self(Name(""), None),
                  declarations.flatMap { i =>
                    if (!i.kind.isConstructor &&
                      !i.isVarSetter &&
                      !isSyntheticMember(i)) toStat(i)
                    else Nil
                  }
                )
              )
            case _ =>
              fail(info)
          }
        case s.TypeSignature(Some(typeParameters), lo, hi) =>
          if (lo.nonEmpty && lo == hi) {
            Defn.Type(
              toMods(info),
              Type.Name(info.displayName),
              typeParameters.smap(toTypeParam),
              toType(lo)
            )
          } else {
            Decl.Type(
              toMods(info),
              Type.Name(info.displayName),
              typeParameters.smap(toTypeParam),
              toTypeBounds(lo, hi)
            )
          }
        case _ =>
          fail(info)
      }
  }

  def unwrapRepeatedType(tpe: s.Type): s.Type = tpe match {
    case s.RepeatedType(targ) =>
      // Workaround for https://github.com/scalameta/scalameta/issues/1497
      s.TypeRef(
        prefix = s.NoType,
        symbol = "scala/collection/Seq#",
        typeArguments = targ :: Nil
      )
    case targ =>
      targ
  }

  def toInit(tpe: s.Type): Init = {
    val fixed = tpe match {
      case ref: s.TypeRef =>
        val newTypeArguments = ref.typeArguments.map(unwrapRepeatedType)
        ref.copy(typeArguments = newTypeArguments)
      case _ =>
        tpe
    }
    Init(
      toType(fixed),
      Name.Anonymous(),
      // Can't support term arguments
      Nil
    )
  }

  def toTypeBounds(lo: s.Type, hi: s.Type): Type.Bounds =
    Type.Bounds(
      Some(lo).filterNot(TypeExtractors.Nothing.matches).map(toType),
      Some(hi).filterNot(TypeExtractors.Any.matches).map(toType)
    )

  def toStat(info: s.SymbolInformation): Option[Stat] = {
    if (info.symbol.contains("$anon")) {
      // Skip these for now, anonymous classes slip into a handful of public signatures and I'm not sure what is
      // best to do with them.
      None
    } else {
      try {
        Some(toTree(info).asInstanceOf[Stat])
      } catch {
        case NonFatal(e) =>
          if (fatalErrors) {
            throw new Exception(info.toString, e) with NoStackTrace
          } else {
            None
          }
      }
    }
  }

  case class TypeToTreeError(msg: String, cause: Option[Throwable] = None)
      extends Exception(msg, cause.orNull)
  def fail(sig: s.Signature): Nothing =
    fail(sig.asMessage)
  def fail(tpe: s.Type): Nothing =
    fail(tpe.asMessage)
  def fail(tree: Tree): Nothing =
    throw TypeToTreeError(tree.syntax + s"\n\n${tree.structure}")
  def fail(any: GeneratedMessage): Nothing =
    throw TypeToTreeError(any.toString)
  def fail(any: GeneratedMessage, cause: Throwable): Nothing =
    throw TypeToTreeError(any.toString, Some(cause))

  def toTermRef(info: s.SymbolInformation): Term.Ref = {
    if (info.kind.isParameter) Term.Name(info.displayName)
    else {
      shorten match {
        case QualifyStrategy.Full =>
          if (info.symbol.isRootPackage) {
            Term.Name("_root_")
          } else if (info.owner.isEmpty) {
            fail(info)
          } else {
            Term.Select(
              toTermRef(this.info(info.owner)),
              Term.Name(info.displayName))
          }
        case QualifyStrategy.Name =>
          imports += info.symbol
          Term.Name(info.displayName)
        case QualifyStrategy.Readable =>
          val owner = this.info(info.owner)
          if (owner.kind.isPackageObject ||
            owner.kind.isPackage ||
            (owner.kind.isObject && info.kind.isType)) {
            imports += info.symbol
            Term.Name(info.displayName)
          } else {
            Term.Select(toTermRef(owner), Term.Name(info.displayName))
          }
      }
    }
  }

  def toTermRef(tpe: s.Type): Term.Ref = tpe match {
    case s.SingleType(prefix, symbol) =>
      prefix match {
        case s.NoType => symbol.toTermName
        case qual => Term.Select(toTermRef(qual), symbol.toTermName)
      }
    case s.ThisType(symbol) =>
      Term.This(symbol.toTermName)
    case s.TypeRef(s.NoType, symbol, Nil) =>
      symbol.toTermName
    case _ =>
      fail(tpe)
  }

  def toTypeRef(info: s.SymbolInformation): Type.Ref = {
    def name = Type.Name(info.displayName)
    if (shorten.isName || info.kind.isTypeParameter) {
      name
    } else {
      val owner = this.info(info.owner)
      if (shorten.isReadable && (
          owner.kind.isPackage ||
          owner.kind.isPackageObject ||
          (owner.kind.isObject && info.kind.isType)
        )) {
        imports += info.symbol
        name
      } else if (owner.kind.isPackage ||
        owner.kind.isObject ||
        info.language.isJava) {
        Type.Select(toTermRef(owner), name)
      } else if (owner.kind.isClass || owner.kind.isTrait) {
        Type.Project(toTypeRef(owner), name)
      } else {
        fail(info)
      }
    }
  }

  def toType(tpe: s.Type): Type = tpe match {
    case s.TypeRef(prefix, symbol, typeArguments) =>
      def name: Type.Name = symbol.toTypeName
      def targs: List[Type] =
        typeArguments.iterator.map {
          case TypeExtractors.Wildcard() =>
            Type.Placeholder(Type.Bounds(None, None))
          case targ =>
            toType(targ)
        }.toList
      symbol match {
        case TypeExtractors.FunctionN() if typeArguments.lengthCompare(0) > 0 =>
          val params :+ res = targs
          Type.Function(params, res)
        case TypeExtractors.TupleN() if typeArguments.lengthCompare(1) > 0 =>
          Type.Tuple(targs)
        case _ =>
          val qual: Type.Ref = prefix match {
            case s.NoType =>
              if (shorten.isName) {
                name
              } else if (symbol.isLocal) {
                name
              } else {
                toTypeRef(info(symbol))
              }
            case _: s.SingleType | _: s.ThisType | _: s.SuperType =>
              Type.Select(toTermRef(prefix), name)
            case _ =>
              Type.Project(toType(prefix), name)
          }
          (qual, targs) match {
            case (q, Nil) => q
            case (name: Type.Name, Seq(lhs, rhs))
                if !Character.isJavaIdentifierPart(name.value.head) =>
              Type.ApplyInfix(lhs, name, rhs)
            case (q, targs) => Type.Apply(q, targs)
          }
      }
    case s.SingleType(_, symbol) =>
      val info = this.info(symbol)
      if (info.kind.isParameter || info.isVal || info.kind.isObject) {
        Type.Singleton(toTermRef(info))
      } else {
        info.signature match {
          case s.MethodSignature(_, _, returnType) =>
            toType(returnType)
          case els =>
            fail(els)
        }
      }
    case s.ThisType(symbol) =>
      Type.Select(
        Term.This(Name.Anonymous()),
        Type.Name(this.info(symbol).displayName)
      )
    case s.SuperType(prefix @ _, symbol) =>
      // TODO: print prefix https://github.com/scalacenter/scalafix/issues/758
      Type.Select(
        Term.Super(Name.Anonymous(), Name.Anonymous()),
        Type.Name(this.info(symbol).displayName)
      )
    case s.ConstantType(_) =>
      toType(tpe.widen)
    case s.ExistentialType(underlying, Some(declarations)) =>
      withHardlinks(declarations.hardlinks) { () =>
        toType(underlying)
      }
    case s.RepeatedType(underlying) =>
      Type.Repeated(toType(underlying))
    case s.ByNameType(underlying) =>
      Type.ByName(toType(underlying))
    case s.AnnotatedType(annots, underlying) =>
      if (!annots.exists(_.tpe.isDefined)) {
        toType(underlying)
      } else {
        Type.Annotate(
          toType(underlying),
          annots.iterator.map { annot =>
            toModAnnot(annot.tpe)
          }.toList
        )
      }
    case s.StructuralType(underlying, Some(scope)) =>
      withHardlinks(scope.hardlinks) { () =>
        val declarations = scope.infos.filterNot(_.symbol.isLocal)
        declarations match {
          case Nil =>
            toType(underlying)
          case decls =>
            val tpe = underlying match {
              case TypeExtractors.AnyRef() => None
              case els => Some(toType(els))
            }
            Type.Refine(
              tpe,
              decls.iterator
                .filterNot(_.isVarSetter)
                .flatMap(toStat)
                .toList
            )
        }
      }
    case s.WithType(types) =>
      val (head, tail) = types.head match {
        case TypeExtractors.AnyRef() if types.lengthCompare(1) > 0 =>
          types(1) -> types.iterator.drop(2)
        case head =>
          head -> types.iterator.drop(1)
      }
      tail.foldLeft(toType(head)) {
        case (accum, next) => Type.With(accum, toType(next))
      }
    case s.UniversalType(Some(typeParameters), underlying) =>
      val universalName = t"T"
      withHardlinks(typeParameters.hardlinks) { () =>
        Type.Project(
          Type.Refine(
            None,
            Defn.Type(
              Nil,
              universalName,
              typeParameters.smap(toTypeParam),
              toType(underlying)
            ) :: Nil
          ),
          universalName
        )
      }
    case _ =>
      fail(tpe)
  }

  def withHardlinks[T](
      holders: Iterable[s.SymbolInformation]
  )(f: () => T): T = {
    hardlinks ++= holders.iterator.map(i => i.symbol -> i)
    val result = f()
    hardlinks --= holders.iterator.map(_.symbol)
    result
  }
  private val hardlinks = mutable.Map.empty[String, s.SymbolInformation]

  def toModAnnot(tpe: s.Type): Mod.Annot = {
    Mod.Annot(toInit(tpe))
  }

  def toTermParam(info: s.SymbolInformation): Term.Param = {
    Term.Param(
      Nil,
      Term.Name(info.displayName),
      Some(toType(info.valueType)),
      None
    )
  }

  def toTypeParam(info: s.SymbolInformation): Type.Param = {
    require(info.kind.isTypeParameter, info.toString)
    val (tparams, bounds) = info.signature match {
      case s.TypeSignature(Some(typeParameters), lo, hi) =>
        val params =
          if (typeParameters.symlinks.isEmpty) {
            typeParameters.hardlinks.map(toTypeParam).toList
          } else {
            typeParameters.symlinks.iterator.map { sym =>
              if (sym.endsWith("[_]")) {
                Type.Param(
                  Nil,
                  Name(""),
                  Nil,
                  Type.Bounds(None, None),
                  Nil,
                  Nil
                )
              } else {
                toTypeParam(this.info(sym))
              }
            }.toList
          }
        params -> toTypeBounds(lo, hi)
      case _ =>
        Nil -> Type.Bounds(None, None)
    }
    Type.Param(
      toMods(info),
      name = Type.Name(info.displayName),
      tparams = tparams,
      tbounds = bounds,
      // TODO: re-sugar context and view bounds https://github.com/scalacenter/scalafix/issues/759
      vbounds = Nil,
      cbounds = Nil
    )
  }

  def toMods(info: s.SymbolInformation): List[Mod] = {
    val buf = List.newBuilder[Mod]
    info.access match {
      case s.PrivateAccess() =>
        buf += Mod.Private(Name.Anonymous())
      case s.PrivateWithinAccess(symbol) =>
        buf += Mod.Private(symbol.toIndeterminateName)
      case s.PrivateThisAccess() =>
        buf += Mod.Private(Term.This(Name.Anonymous()))
      case s.ProtectedAccess() =>
        buf += Mod.Protected(Name.Anonymous())
      case s.ProtectedWithinAccess(symbol) =>
        buf += Mod.Protected(symbol.toIndeterminateName)
      case s.ProtectedThisAccess() =>
        buf += Mod.Protected(Term.This(Name.Anonymous()))
      case _ =>
    }
    if (info.is(p.SEALED)) buf += Mod.Sealed()
    if (info.kind.isClass && info.is(p.ABSTRACT)) buf += Mod.Abstract()
    if (info.is(p.FINAL) && !info.kind.isObject && !info.kind.isPackageObject)
      buf += Mod.Final()
    if (info.is(p.IMPLICIT)) buf += Mod.Implicit()
    if (info.kind.isClass && info.is(p.CASE)) buf += Mod.Case()
    buf.result()
  }

}
