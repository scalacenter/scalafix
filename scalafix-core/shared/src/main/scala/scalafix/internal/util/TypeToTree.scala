package scalafix.internal.util

import java.io.PrintWriter
import java.io.StringWriter
import org.langmeta.internal.semanticdb._
import scala.meta._
import scala.meta.internal.{semanticdb3 => s}
import scala.reflect.ClassTag
import scala.util.control.NonFatal
import scalafix._
import scalafix.internal.util.SymbolOps.SymbolType
import org.langmeta.semanticdb.Symbol
import scala.meta.internal.semanticdb3.SymbolInformation.{Kind => k}
import scala.meta.internal.semanticdb3.SymbolInformation.{Property => p}
import scala.meta.internal.semanticdb3.Type.{Tag => t}
import TypeSyntax._
import scalapb.GeneratedMessage
import TypeSyntax._
import scala.meta.internal.semanticdb3.Scala._

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
    def smap[T](f: s.SymbolInformation => T): List[T] =
      syms.iterator.map(info).map(f).toList
  }

  private implicit class XtensionSchemaType(tpe: s.Type) {
    def widen: s.Type = {
      def ref(sym: String): s.Type = {
        s.Type(s.Type.Tag.TYPE_REF, typeRef = Some(s.TypeRef(symbol = sym)))
      }
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
            mods(info),
            Pat.Var(Term.Name(info.name)) :: Nil,
            toType(ret)
          )
        } else if (info.isVar && info.isVarSetter) {
          Decl.Var(
            mods(info),
            Pat.Var(Term.Name(info.name)) :: Nil,
            toType(ret)
          )
        } else {
          Decl.Def(
            mods(info),
            Term.Name(info.name),
            tparams.smap(tparam),
            paramss.iterator.map(params => params.symbols.smap(param)).toList,
            toType(ret)
          )
        }
      case t.CLASS_INFO_TYPE =>
        val Some(s.ClassInfoType(typeParameters, parents, declarations)) =
          info.typ.classInfoType
        info.kind match {
          case k.TRAIT =>
            Defn.Trait(
              mods(info),
              Type.Name(info.name),
              Nil, // TODO
              Ctor.Primary(Nil, Name(""), Nil),
              Template(
                Nil,
                Nil,
                Self(Name(""), None),
                declarations.smap(toStat)
              )
            )
        }
      case t.TYPE_TYPE =>
        val Some(s.TypeType(typeParameters, lo, hi)) = info.typ.typeType
        Decl.Type(
          mods(info),
          Type.Name(info.name),
          typeParameters.smap(tparam),
          toTypeBounds(lo, hi)
        )
    }
    Result(tree, Nil)
  }

  def toTypeBounds(lo: Option[s.Type], hi: Option[s.Type]): Type.Bounds =
    Type.Bounds(
      lo.filterNot(T.Nothing.matches).map(toType),
      hi.filterNot(T.Any.matches).map(toType)
    )

  def toStat(info: s.SymbolInformation): Stat = {
    toTree(info).tree.asInstanceOf[Stat]
  }

  def fail(any: GeneratedMessage): Nothing = sys.error(any.toProtoString)

  def toTermRef(curr: s.SymbolInformation): Term.Ref = {
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

  def toType(tpe: s.Type): Type = {
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
                  val curr = info(symbol)
                  if (curr.kind.isTypeParameter) {
                    name
                  } else {
                    val owner = info(curr.owner)
                    if (shorten.isReadable && (
                        owner.kind.isPackage ||
                        (owner.kind.isObject && curr.kind.isType)
                      )) {
                      name
                    } else if (owner.kind.isObject || curr.language.isJava) {
                      Type.Select(toTermRef(owner), name)
                    } else if (owner.kind.isClass || owner.kind.isTrait) {
                      Type.Project(toType(owner.typ), name)
                    } else {
                      fail(curr)
                    }
                  }
                }
            }
            if (typeArguments.isEmpty) qual
            else Type.Apply(qual, targs)
        }
    }
  }

  def param(info: s.SymbolInformation): Term.Param = {
    Term.Param(
      Nil,
      Term.Name(info.name),
      Some(toType(info.typ)),
      None
    )
  }

  def tparam(info: s.SymbolInformation): Type.Param = {
    require(info.kind.isTypeParameter, info.toProtoString)
    Type.Param(
      mods = Nil,
      name = Type.Name(info.name),
      tparams = Nil,
      tbounds = Type.Bounds(None, None),
      vbounds = Nil,
      cbounds = Nil
    )
  }

  def mods(info: s.SymbolInformation): List[Mod] = {
    val buf = List.newBuilder[Mod]
    info.accessibility.foreach { accessibility =>
      if (accessibility.tag.isPrivate) buf += Mod.Private(Name.Anonymous())
      if (accessibility.tag.isProtected) buf += Mod.Protected(Name.Anonymous())
    // TODO: within
    }
    if (info.kind.isClass && info.is(p.ABSTRACT)) buf += Mod.Abstract()
    if (info.is(p.FINAL)) buf += Mod.Final()
    if (info.is(p.IMPLICIT)) buf += Mod.Implicit()
    if (info.is(p.SEALED)) buf += Mod.Sealed()
    if (info.kind.isClass && info.is(p.CASE)) buf += Mod.Case()
    buf.result()
  }

}
