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
import scala.meta.internal.semanticdb3.SingletonType.{Tag => x}
import scala.meta.internal.semanticdb3.SymbolInformation.{Kind => k}
import scala.meta.internal.semanticdb3.SymbolInformation.{Property => p}
import scala.meta.internal.semanticdb3.Type.{Tag => t}
import TypeSyntax._
import scalapb.GeneratedMessage

object TypeSyntax {
  def prettify(
      stpe: s.Type,
      ctx: RuleCtx,
      shortenNames: Boolean,
      pos: Position)(implicit index: SemanticdbIndex): Option[(Type, Patch)] = {
    try {
      val table = index.asInstanceOf[EagerInMemorySemanticdbIndex].table
      Some(new TypeSyntax(stpe, ctx, shortenNames, pos, table).prettify())
    } catch {
      case NonFatal(e) =>
        val msg = new StringWriter()
        e.printStackTrace(new PrintWriter(msg))
        ctx.config.reporter.error(msg.toString, pos)
        None
    }
  }

  implicit class XtensionSymbolInformationProperties(
      info: s.SymbolInformation) {
    private def test(property: s.SymbolInformation.Property): Boolean =
      (info.properties & property.value) != 0
    def isVal: Boolean = test(p.VAL)
    def isVar: Boolean = test(p.VAR)
    def isVarSetter: Boolean =
      isVar && info.name.endsWith("_=")
  }

  object T {
    abstract class TypeRefExtractor(sym: String) {
      def matches(tpe: s.Type): Boolean = tpe.tag match {
        case t.TYPE_REF =>
          tpe.typeRef.exists(_.symbol == sym)
        case t.WITH_TYPE =>
          tpe.withType.exists { x =>
            x.types.lengthCompare(1) == 0 &&
            unapply(x.types.head)
          }
        case _ =>
          false
      }
      def unapply(tpe: s.Type): Boolean = matches(tpe)
    }
    object AnyRef extends TypeRefExtractor("scala.AnyRef#")
    object Nothing extends TypeRefExtractor("scala.Nothing#")
    object Any extends TypeRefExtractor("scala.Any#")
  }

  def isFunctionN(symbol: String): Boolean = {
    symbol.startsWith("scala.Function") &&
    symbol.endsWith("#")
  }

  def isTupleN(symbol: String): Boolean = {
    symbol.startsWith("scala.Tuple") &&
    symbol.endsWith("#")
  }

  object FunctionN {
    def unapply(symbol: String): Boolean = isFunctionN(symbol)
  }
  object TupleN {
    def unapply(symbol: String): Boolean = isTupleN(symbol)
  }
}

class TypeSyntax private (
    rtpe: s.Type,
    ctx: RuleCtx,
    shortenNames: Boolean,
    pos: Position,
    _table: SymbolTable
)(implicit index: SemanticdbIndex) {
  val table = new CombinedSymbolTable(index, _table)

  def fail(any: GeneratedMessage): Nothing = sys.error(any.toProtoString)

  def isStable(symbol: Symbol): Boolean = {
    def loop(symbol: Symbol): Boolean = symbol match {
      case Symbol.None => true
      case Symbol.Global(owner, Signature.Term(_)) => loop(owner)
      case _ => false
    }
    symbol match {
      case Symbol.Global(owner, Signature.Term(_) | Signature.Type(_)) =>
        loop(owner)
      case _ =>
        false
    }
  }

  def ref(sym: String): s.Type = {
    s.Type(s.Type.Tag.TYPE_REF, typeRef = Some(s.TypeRef(symbol = sym)))
  }

  def widen(tpe: s.Type): s.Type = {
    tpe.tag match {
      case t.SINGLETON_TYPE =>
        import s.SingletonType.{Tag => x}
        val singletonType = tpe.singletonType.get
        singletonType.tag match {
          case x.SYMBOL =>
            index.denotation(Symbol(singletonType.symbol)).get.tpeInternal.get
          case x.BOOLEAN => ref("scala.Boolean#")
          case x.BYTE => ref("scala.Byte#")
          case x.CHAR => ref("scala.Char#")
          case x.DOUBLE => ref("scala.Double#")
          case x.FLOAT => ref("scala.Float#")
          case x.INT => ref("scala.Int#")
          case x.LONG => ref("scala.Long#")
          case x.NULL => ref("scala.Null#")
          case x.SHORT => ref("scala.Short#")
          case x.STRING => ref("java.lang.String#")
          case x.UNIT => ref("scala.Unit#")
          case x.SUPER => tpe
          case x.THIS => tpe
          case x.UNKNOWN_SINGLETON => tpe
          case x.Unrecognized(_) => tpe
        }
      // TODO: handle non-singleton widening.
      case _ => tpe
    }
  }

  /*
   * Returns a scala.meta.Tree given a scala.meta.Symbol.
   *
   * NOTE: this method has become an utter mess and is in need of a clean reimplementation.
   * The reason it became a mess is because it was first implemented before SymbolInformation
   * existed so it tries to infer to much information from the `Symbol.structure`. Now that
   * we have SymbolInformation it makes more sense to have a `infoToTree(SymbolInformation): Tree`.
   *
   * Before: _root_.scala.Predef.Set#
   * After: Type.Select(Term.Select(Term.Name("scala"), Term.Name("Predef")),
   *                    Type.Name("Set"))
   */
  def symbolToTree(sym: Symbol.Global): (Patch, Tree) = {
    var patch = Patch.empty
    def loop[T: ClassTag](symbol: Symbol): T = {
      val result = symbol match {
        // base case, symbol `_root_.`  becomes `_root_` term
        case Symbol.Global(Symbol.None, Signature.Term(name)) =>
          Term.Name(name)
        // symbol `A#B#`  becomes `A#B` type
        case Symbol.Global(owner @ SymbolType(), Signature.Type(name))
            if !table.info(symbol.syntax).language.isJava =>
          Type.Project(loop[Type](owner), Type.Name(name))
        // symbol `A#B#`  becomes `A#B` type
        case sym @ Symbol.Global(owner, sig @ Signature.Term(name)) =>
          if (shortenNames && isStable(owner)) {
            val termName = Term.Name(name)
            patch += ctx.addGlobalImport(symbol)
            termName
          } else {
            Term.Select(loop[Term.Ref](owner), Term.Name(name))
          }
        // symbol `a.B#`  becomes `a.B` type
        case Symbol.Global(owner, Signature.Type(name)) =>
          if (shortenNames && isStable(owner)) {
            val typeName = Type.Name(name)
            patch += ctx.addGlobalImport(symbol)
            typeName
          } else {
            Type.Select(loop[Term.Ref](owner), Type.Name(name))
          }

        case Symbol.Global(_, Signature.TypeParameter(name)) =>
          Type.Name(name)
      }
      try {
        result.asInstanceOf[T]
      } catch {
        case e: ClassCastException =>
          throw new IllegalArgumentException(symbol.syntax, e)
      }
    }
    val tpe = loop[Tree](sym)
    patch -> tpe
  }

  var patch = Patch.empty

  def toTermRef(tpe: s.Type): Term.Ref = tpe.tag match {
    case t.SINGLETON_TYPE =>
      val singleton = tpe.singletonType.get
      def name = Term.Name(table.info(singleton.symbol).name)
      singleton.tag match {
        case x.SYMBOL =>
          singleton.prefix match {
            case Some(qual) => Term.Select(toTermRef(qual), name)
            case _ => name
          }
        case x.THIS =>
          assert(singleton.prefix.isEmpty, singleton.prefix.get.toProtoString)
          Term.This(name)
        case _ =>
          fail(tpe)
      }
    case _ =>
      fail(tpe)
  }

  def toTermParam(info: s.SymbolInformation): Term.Param = {
    Term.Param(
      Nil,
      Term.Name(info.name),
      None,
      None
    )
  }

  def toTypeParam(info: s.SymbolInformation): Type.Param = {
    Type.Param(
      Nil,
      Type.Name(info.name),
      Nil,
      Type.Bounds(None, None),
      Nil,
      Nil
    )
  }

  def toStat(info: s.SymbolInformation): Stat = {
    val tpe = info.tpe.get
    tpe.tag match {
      case t.METHOD_TYPE =>
        val Some(s.MethodType(tparams, params, Some(ret))) = tpe.methodType
        if (info.isVal) {
          Decl.Val(
            Nil,
            Pat.Var(Term.Name(info.name)) :: Nil,
            toType(ret)
          )
        } else if (info.isVar) {
          Decl.Var(
            Nil,
            Pat.Var(Term.Name(info.name)) :: Nil,
            toType(ret)
          )
        } else {
          Decl.Def(
            Nil,
            Term.Name(info.name),
            tparams.iterator.map(table.info).map(toTypeParam).toList,
            params.iterator
              .map(_.symbols.iterator.map(table.info).map(toTermParam).toList)
              .toList,
            toType(ret)
          )
        }
      case t.TYPE_TYPE =>
        val Some(s.TypeType(typeParameters, lo, hi)) = info.tpe.get.typeType
        Decl.Type(
          Nil,
          Type.Name(info.name),
          typeParameters.iterator.map(table.info).map(toTypeParam).toList,
          Type.Bounds(
            lo.filterNot(T.Nothing.matches).map(toType),
            hi.filterNot(T.Any.matches).map(toType)
          )
        )
      case _ =>
        fail(tpe)
    }
  }

  def toTypeRef(symbol: String): Type.Ref = {
    val (p, t) = symbolToTree(Symbol(symbol).asInstanceOf[Symbol.Global])
    patch += p
    t.asInstanceOf[Type.Ref]
  }

  def toType(tpe: s.Type): Type = tpe.tag match {
    case t.TYPE_REF =>
      val Some(s.TypeRef(prefix, symbol, typeArguments)) = tpe.typeRef
      val info = table.info(symbol)
      def name = Type.Name(info.name)
      def targs = typeArguments.iterator.map(toType).toList
      symbol match {
        case FunctionN() =>
          val params :+ res = targs
          Type.Function(params, res)
        case TupleN() =>
          Type.Tuple(targs)
        case _ =>
          val qual = prefix match {
            case Some(p) =>
              p.tag match {
                case t.SINGLETON_TYPE =>
                  Type.Select(toTermRef(p), name)
                case t.TYPE_REF =>
                  Type.Project(toType(p), name)
                case _ => fail(p)
              }
            case _ =>
              info.kind match {
                case k.TYPE_PARAMETER => name
                case k.TYPE if !isStable(Symbol(symbol)) =>
                  val owner = table.info(info.owner)
                  // HACK: here we try to distinguish between existential type parameters
                  // and A#B#C paths. This is need rethinking.
                  owner.kind match {
                    case k.FIELD =>
                      name
                    case _ =>
                      toTypeRef(symbol)
                  }
                case _ =>
                  if (isSyntheticName(info.name)) {
                    val bounds = for {
                      tpe <- info.tpe
                      tpetpe <- tpe.typeType
                      lower = tpetpe.lowerBound
                        .filterNot(T.Nothing.matches)
                        .map(toType)
                      upper = tpetpe.upperBound
                        .filterNot(T.Any.matches)
                        .map(toType)
                    } yield Type.Bounds(lower, upper)
                    Type.Placeholder(bounds.getOrElse(Type.Bounds(None, None)))
                  } else {
                    toTypeRef(symbol)
                  }
              }
          }
          typeArguments match {
            case Nil => qual
            case _ => Type.Apply(qual, targs)
          }
      }
    case t.SINGLETON_TYPE =>
      val singleton = tpe.singletonType.get
      singleton.tag match {
        case x.SYMBOL =>
          val info = table.info(singleton.symbol)
          info.kind match {
            case k.LOCAL => Type.Singleton(Term.Name(info.name))
            case k.METHOD =>
              val ref = singleton.prefix match {
                case Some(p) =>
                  Term.Select(toTermRef(p), Term.Name(info.name))
                case _ =>
                  Term.Name(info.name)
              }
              Type.Singleton(ref)
            case _ =>
              Symbol(info.symbol) match {
                case g: Symbol.Global =>
                  val (p, x) = symbolToTree(g)
                  patch += p
                  Type.Singleton(x.asInstanceOf[Term.Ref])
                case _ =>
                  fail(singleton)
              }
          }
        case x.THIS =>
          assert(singleton.prefix.isEmpty, singleton.prefix.get.toProtoString)
          val name = Name.Indeterminate(table.info(singleton.symbol).name)
          Type.Singleton(Term.This(name))
        case _ =>
          toType(widen(tpe))
      }
    case t.STRUCTURAL_TYPE =>
      val structural = tpe.structuralType.get
      // TODO: handle local decls, here we widen the type which may cause compilation errors
      // if the refinement declarations are referenced via scala.language.reflectiveCalls.
      val declarations =
        structural.declarations.filterNot(_.startsWith("local"))
      declarations match {
        case Nil =>
          toType(structural.tpe.get)
        case decls =>
          val tpe = structural.tpe match {
            case Some(T.AnyRef()) => None
            case els => els.map(toType)
          }
          Type.Refine(
            tpe,
            decls.iterator
              .map(table.info)
              .filterNot(_.isVarSetter)
              .map(toStat)
              .toList
          )
      }
    case t.WITH_TYPE =>
      val Some(s.WithType(types)) = tpe.withType
      val (head, tail) = types.head match {
        case T.AnyRef() if types.lengthCompare(1) > 0 =>
          types(1) -> types.iterator.drop(2)
        case head =>
          head -> types.iterator.drop(1)
      }
      tail.foldLeft(toType(head)) {
        case (accum, next) => Type.With(accum, toType(next))
      }
    case t.ANNOTATED_TYPE =>
      val Some(s.AnnotatedType(_, Some(t))) = tpe.annotatedType
      // Discard type parameters because we don't have the term arguments, which makes it
      // impossible to represent the type `T @ann[Int](42)`, it would only print as
      // `T @ann[Int]`, which does not compile.
      toType(t)
    case t.EXISTENTIAL_TYPE =>
      val Some(s.ExistentialType(typeParameters, Some(t))) =
        tpe.existentialType
      if (typeParameters.forall(isSyntheticName)) {
        toType(t)
      } else {
        Type.Existential(
          toType(t),
          typeParameters.iterator.map(table.info).map(toStat).toList
        )
      }
    case _ =>
      fail(tpe)
  }

  def isSyntheticName(name: String): Boolean = name.indexOf('$') != -1

  case class Failed(msg: String) extends Exception(msg)

  def prettify(): (Type, Patch) = {
    val toConvert = rtpe.tag match {
      case t.METHOD_TYPE =>
        val Some(s.MethodType(_, _, Some(returnType))) = rtpe.methodType
        returnType
      case _ =>
        rtpe
    }
    (toType(toConvert), patch)
  }
}
