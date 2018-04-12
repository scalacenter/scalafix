package scalafix.internal.util

import scala.meta._
import scala.meta.internal.semanticdb3.Scala._
import scala.meta.internal.semanticdb3.SymbolInformation.{Kind => k}
import scala.meta.internal.semanticdb3.SymbolInformation.{Property => p}
import scala.meta.internal.semanticdb3.Type.{Tag => t}
import scala.meta.internal.{semanticdb3 => s}
import scalafix.internal.util.TypeSyntax._
import scalapb.GeneratedMessage

case class Result(tree: Tree, imports: List[String])

sealed abstract class Shorten {
  def isReadable: Boolean = this == Shorten.Readable
  def isUpToRoot: Boolean = this == Shorten.UpToRoot
  def isNameOnly: Boolean = this == Shorten.NameOnly
}
object Shorten {

  /** Fully quality up to _root_ package */
  case object UpToRoot extends Shorten

  /** Optimize for human-readability */
  case object Readable extends Shorten

  /** Discard prefix and use short name only */
  case object NameOnly extends Shorten

}

class TypeToTree(table: SymbolTable, shorten: Shorten) {

  private implicit class XtensionSymbolInformationProperties(
      info: s.SymbolInformation) {
    def typ: s.Type =
      info.tpe.getOrElse(throw new IllegalArgumentException(info.toProtoString))
    def is(property: s.SymbolInformation.Property): Boolean =
      (info.properties & property.value) != 0
    def isVal: Boolean = is(p.VAL)
    def isVar: Boolean = is(p.VAR)
    def isVarSetter: Boolean =
      isVar && info.name.endsWith("_=")
  }
  private implicit class XtensionSymbolInfo(sym: String) {
    def toTermName: Term.Name = Term.Name(info(sym).name)
    def toTypeName: Type.Name = Type.Name(info(sym).name)
  }

  private implicit class XtensionSymbols(syms: Seq[String]) {
    def scollect[T](f: PartialFunction[s.SymbolInformation, T]): List[T] =
      syms.iterator.map(info).collect(f).toList
    def smap[T](f: s.SymbolInformation => T): List[T] =
      syms.iterator.map(info).map(f).toList
  }

  def ref(sym: String): s.Type = {
    s.Type(s.Type.Tag.TYPE_REF, typeRef = Some(s.TypeRef(symbol = sym)))
  }

  private implicit class XtensionSchemaType(tpe: s.Type) {
    def widen: s.Type = {
      tpe.tag match {
        case t.SINGLETON_TYPE =>
          import s.SingletonType.Tag
          val singletonType = tpe.singletonType.get
          singletonType.tag match {
            case Tag.SYMBOL => info(singletonType.symbol).typ
            case Tag.BOOLEAN => ref("scala.Boolean#")
            case Tag.BYTE => ref("scala.Byte#")
            case Tag.CHAR => ref("scala.Char#")
            case Tag.DOUBLE => ref("scala.Double#")
            case Tag.FLOAT => ref("scala.Float#")
            case Tag.INT => ref("scala.Int#")
            case Tag.LONG => ref("scala.Long#")
            case Tag.NULL => ref("scala.Null#")
            case Tag.SHORT => ref("scala.Short#")
            case Tag.STRING => ref("java.lang.String#")
            case Tag.UNIT => ref("scala.Unit#")
            case Tag.SUPER => tpe
            case Tag.THIS => tpe
            case Tag.UNKNOWN_SINGLETON => tpe
            case Tag.Unrecognized(_) => tpe
          }
        // TODO: handle non-singleton widening.
        case _ => tpe
      }
    }
  }

  private def info(sym: String): s.SymbolInformation = {
    table.info(sym).getOrElse(throw new NoSuchElementException(sym))
  }

  def toTree(info: s.SymbolInformation): Result = {
    val tree = info.typ.tag match {
      case t.METHOD_TYPE =>
        val Some(s.MethodType(tparams, paramss, Some(ret))) =
          info.typ.methodType
        if (info.isVal) {
          Decl.Val(
            toMods(info),
            Pat.Var(Term.Name(info.name)) :: Nil,
            toType(ret)
          )
        } else if (info.isVar && !info.isVarSetter) {
          Decl.Var(
            toMods(info),
            Pat.Var(Term.Name(info.name)) :: Nil,
            toType(ret)
          )
        } else {
          Decl.Def(
            toMods(info),
            Term.Name(info.name),
            tparams.smap(toTypeParam),
            paramss.iterator
              .map(params => params.symbols.smap(toTermParam))
              .toList,
            toType(ret)
          )
        }
      case t.CLASS_INFO_TYPE =>
        val Some(s.ClassInfoType(typeParameters, parents, declarations)) =
          info.typ.classInfoType
        val isCaseClass = info.is(p.CASE)

        def inits =
          parents.iterator
            .filterNot {
              case T.AnyRef() | T.Any() => true
              case parent =>
                if (isCaseClass) {
                  parent match {
                    case T.Product() | T.Serializable() =>
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
          case k.TRAIT =>
            Defn.Trait(
              toMods(info),
              Type.Name(info.name),
              typeParameters.smap(toTypeParam),
              Ctor.Primary(Nil, Name(""), Nil),
              Template(
                Nil,
                inits,
                Self(Name(""), None),
                declarations.scollect {
                  case i
                      if !i.kind.isConstructor &&
                        !i.isVarSetter =>
                    toStat(i)
                }
              )
            )
          case k.CLASS =>
            val decls = declarations.map(this.info)
            val primaryConstructor = decls.collectFirst {
              case i if i.kind.isConstructor && i.is(p.PRIMARY) =>
                i
            }
            val ctor = primaryConstructor match {
              case Some(i) =>
                toStat(i) match {
                  case d: Decl.Def =>
                    val paramss = d.paramss match {
                      case Nil :: Nil if !info.is(p.CASE) => Nil
                      case els => els
                    }

                    Ctor.Primary(
                      toMods(i),
                      Name.Anonymous(),
                      paramss
                    )
                  case els =>
                    fail(els)
                }
              case _ =>
                Ctor.Primary(Nil, Name(""), Nil)
            }

            def isSyntheticMember(m: s.SymbolInformation) =
              isCaseClass == caseClassMethods(m.name)
            Defn.Class(
              toMods(info),
              Type.Name(info.name),
              typeParameters.smap(toTypeParam),
              ctor,
              Template(
                Nil,
                inits,
                Self(Name(""), None),
                declarations.scollect {
                  case i
                      if !i.kind.isConstructor &&
                        !i.kind.isField &&
                        !i.isVarSetter &&
                        !isSyntheticMember(i) =>
                    toStat(i)
                }
              )
            )
          case _ =>
            fail(info)
        }
      case t.TYPE_TYPE =>
        val Some(s.TypeType(typeParameters, lo, hi)) = info.typ.typeType
        Decl.Type(
          toMods(info),
          Type.Name(info.name),
          typeParameters.smap(toTypeParam),
          toTypeBounds(lo, hi)
        )
      case _ =>
        fail(info)
    }
    Result(tree, Nil)
  }

  def toInit(tpe: s.Type): Init = {
    // Can't support term arguments
    Init(toType(tpe), Name.Anonymous(), Nil)
  }

  def toTypeBounds(lo: Option[s.Type], hi: Option[s.Type]): Type.Bounds =
    Type.Bounds(
      lo.filterNot(T.Nothing.matches).map(toType),
      hi.filterNot(T.Any.matches).map(toType)
    )

  def toStat(info: s.SymbolInformation): Stat = {
    toTree(info).tree.asInstanceOf[Stat]
  }

  def fail(tree: Tree): Nothing =
    sys.error(tree.syntax + s"\n\n${tree.structure}")
  def fail(any: GeneratedMessage): Nothing = sys.error(any.toProtoString)

  def toTermRef(curr: s.SymbolInformation): Term.Ref = {
    if (curr.kind.isParameter) Term.Name(curr.name)
    else {
      shorten match {
        case Shorten.UpToRoot =>
          if (curr.symbol.isRootPackage) Term.Name("_root_")
          else Term.Select(toTermRef(info(curr.owner)), Term.Name(curr.name))
        case Shorten.NameOnly =>
          Term.Name(curr.name)
        case Shorten.Readable =>
          val owner = info(curr.owner)
          if (owner.kind.isPackageObject ||
            owner.kind.isPackage ||
            (owner.kind.isObject && curr.kind.isType)) {
            Term.Name(curr.name)
          } else {
            Term.Select(toTermRef(owner), Term.Name(curr.name))
          }
      }
    }
  }

  def toTermRef(tpe: s.Type): Term.Ref = tpe.tag match {
    case t.SINGLETON_TYPE =>
      import s.SingletonType.Tag
      val singleton = tpe.singletonType.get
      def name = singleton.symbol.toTermName
      singleton.tag match {
        case Tag.SYMBOL =>
          singleton.prefix match {
            case Some(qual) => Term.Select(toTermRef(qual), name)
            case _ => name
          }
        case Tag.THIS =>
          assert(singleton.prefix.isEmpty, singleton.prefix.get.toProtoString)
          Term.This(name)
        case _ =>
          fail(tpe)
      }
    case _ =>
      fail(tpe)
  }

  def toTypeRef(info: s.SymbolInformation): Type.Ref = {
    def name = Type.Name(info.name)
    if (info.kind.isTypeParameter) {
      name
    } else {
      val owner = this.info(info.owner)
      if (shorten.isReadable && (
          owner.kind.isPackage ||
          owner.kind.isPackageObject ||
          (owner.kind.isObject && info.kind.isType)
        )) {
        name
      } else if (owner.kind.isObject || info.language.isJava) {
        Type.Select(toTermRef(owner), name)
      } else if (owner.kind.isClass || owner.kind.isTrait) {
        Type.Project(toType(owner.typ), name)
      } else {
        fail(info)
      }
    }
  }

  def toType(tpe: s.Type): Type =
    tpe.tag match {
      case t.TYPE_REF =>
        val Some(s.TypeRef(prefix, symbol, typeArguments)) = tpe.typeRef
        def name = symbol.toTypeName
        def targs = typeArguments.iterator.map(toType).toList
        symbol match {
          case FunctionN() =>
            val params :+ res = targs
            Type.Function(params, res)
          case TupleN() =>
            Type.Tuple(targs)
          case _ =>
            val qual: Type.Ref = prefix match {
              case Some(pre) =>
                if (pre.tag.isSingletonType) {
                  Type.Select(toTermRef(pre), name)
                } else {
                  Type.Project(toType(pre), name)
                }
              case _ =>
                if (shorten.isNameOnly) name
                else {
                  toTypeRef(info(symbol))

                }
            }
            if (typeArguments.isEmpty) qual
            else Type.Apply(qual, targs)
        }
      case t.SINGLETON_TYPE =>
        import s.SingletonType.Tag
        val singleton = tpe.singletonType.get
        singleton.tag match {
          case Tag.SYMBOL =>
            val info = this.info(singleton.symbol)
            if (info.kind.isParameter || info.isVal || info.kind.isObject) {
              Type.Singleton(toTermRef(info))
            } else {
              val tpe = info.typ
              tpe.tag match {
                case t.METHOD_TYPE =>
                  val ret = tpe.methodType.get.returnType.get
                  toType(ret)
                case _ =>
                  fail(tpe)
              }
            }
          case Tag.THIS | Tag.SUPER =>
            fail(tpe)
          case _ =>
            toType(tpe.widen)
        }
      case t.EXISTENTIAL_TYPE =>
        // Unsupported
        fail(tpe)
      case _ =>
        fail(tpe)
    }

  private val typePlaceholder = "localPlaceholder"
  private val typePlaceholderType = ref("localPlaceholder")

  def isPlaceholder(sym: String): Boolean =
    sym.startsWith("local") ||
      sym.contains("$")

  def toTermParam(info: s.SymbolInformation): Term.Param = {
    Term.Param(
      Nil,
      Term.Name(info.name),
      Some(toType(info.typ)),
      None
    )
  }

  def toTypeParam(info: s.SymbolInformation): Type.Param = {
    require(info.kind.isTypeParameter, info.toProtoString)
    val tpe = info.typ
    val bounds = tpe.tag match {
      case t.TYPE_TYPE =>
        val Some(s.TypeType(_, lo, hi)) = tpe.typeType
        toTypeBounds(lo, hi)
      case _ =>
        Type.Bounds(None, None)
    }
    Type.Param(
      mods = Nil,
      name = Type.Name(info.name),
      tparams = Nil,
      tbounds = bounds,
      vbounds = Nil,
      cbounds = Nil
    )
  }

  def toMods(info: s.SymbolInformation): List[Mod] = {
    val buf = List.newBuilder[Mod]
    info.accessibility.foreach { accessibility =>
      // TODO: within
      if (accessibility.tag.isPrivate) buf += Mod.Private(Name.Anonymous())
      if (accessibility.tag.isProtected) buf += Mod.Protected(Name.Anonymous())
    }
    if (info.is(p.SEALED)) buf += Mod.Sealed()
    if (info.kind.isClass && info.is(p.ABSTRACT)) buf += Mod.Abstract()
    if (info.is(p.FINAL)) buf += Mod.Final()
    if (info.is(p.IMPLICIT)) buf += Mod.Implicit()
    if (info.kind.isClass && info.is(p.CASE)) buf += Mod.Case()
    buf.result()
  }

  // TODO: workaround for https://github.com/scalameta/scalameta/issues/1492
  val caseClassMethods = Set(
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

}
