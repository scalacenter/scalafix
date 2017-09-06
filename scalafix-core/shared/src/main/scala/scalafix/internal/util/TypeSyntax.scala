package scalafix.internal.util

import scala.meta._
import scala.reflect.ClassTag
import scalafix._
import scalafix.internal.util.SymbolOps.SymbolType
import scalafix.util.SymbolMatcher
import scalafix.util.TreeExtractors._
import org.langmeta.semanticdb.Symbol

object TypeSyntax {
  def prettify(tpe: Type, ctx: RuleCtx, shortenNames: Boolean)(
      implicit index: SemanticdbIndex): (Type, Patch) = {

    val functionN: SymbolMatcher = SymbolMatcher.exact(
      1.to(22).map(i => Symbol(s"_root_.scala.Function$i#")): _*
    )
    val tupleN: SymbolMatcher = SymbolMatcher.exact(
      1.to(22).map(i => Symbol(s"_root_.scala.Tuple$i#")): _*
    )

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

    def stableRef(sym: Symbol): (Patch, Tree) = {
      var patch = Patch.empty
      def loop[T: ClassTag](symbol: Symbol): T = {
        val result = symbol match {
          case Symbol.Global(Symbol.None, Signature.Term(name)) =>
            Term.Name(name)
          case Symbol.Global(owner @ SymbolType(), Signature.Type(name)) =>
            Type.Project(loop[Type](owner), Type.Name(name))
          case Symbol.Global(owner, Signature.Term(name)) =>
            if (shortenNames && isStable(owner)) {
              patch += ctx.addGlobalImport(symbol)
              Term.Name(name)
            } else {
              Term.Select(loop[Term.Ref](owner), Term.Name(name))
            }
          case Symbol.Global(owner, Signature.Type(name)) =>
            if (shortenNames && isStable(owner)) {
              patch += ctx.addGlobalImport(symbol)
              Type.Name(name)
            } else Type.Select(loop[Term.Ref](owner), Type.Name(name))

          case Symbol.Global(_, Signature.TypeParameter(name)) =>
            Type.Name(name)
        }
        result.asInstanceOf[T]
      }
      val tpe = loop[Tree](sym)
      patch -> tpe
    }

    var patch = Patch.empty
    object transformer extends Transformer {
      def typeMismatch[T](obtained: Any, expected: ClassTag[T]): Nothing =
        throw new IllegalArgumentException(
          s"""Expected $expected.
             |Obtained $obtained: ${obtained.getClass}""".stripMargin
        )
      def apply_![T](tree: Tree)(implicit ev: ClassTag[T]): T =
        super.apply(tree) match {
          case e: T => e
          case els => typeMismatch(els, ev)
        }
      def apply_![T](tpes: List[Type])(implicit ev: ClassTag[T]): List[T] =
        super.apply(tpes).collect {
          case tpe: T => tpe
          case els => typeMismatch(els, ev)
        }
      override def apply(tree: Tree): Tree = tree match {
        case Type.Apply(functionN(_), args) =>
          val rargs = apply_![Type](args)
          Type.Function(rargs.init, rargs.last)
        case Type.Apply(tupleN(_), args) =>
          val rargs = apply_![Type](args)
          Type.Tuple(rargs)
        case Name(_) :&&: index.Symbol(sym) =>
          val (addImport, t) = stableRef(sym)
          patch += addImport
          t
        case Type.Select(qual, name) =>
          Type.Select(apply_![Term.Ref](qual), name)
        case els =>
          super.apply(els)
      }
    }
    val result = transformer.apply(tpe).asInstanceOf[Type]
    result -> patch
  }
}
