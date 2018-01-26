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
      0.to(22).map(i => Symbol(s"_root_.scala.Function$i#")): _*
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

    /*
     * Returns a scala.meta.Tree given a scala.meta.Symbol.
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
          case Symbol.Global(owner @ SymbolType(), Signature.Type(name)) =>
            Type.Project(loop[Type](owner), Type.Name(name))
          // symbol `A#B#`  becomes `A#B` type
          case Symbol.Global(owner, Signature.Term(name)) =>
            // TODO(olafur) implement lookup utility to query what symbol signature resolves to.
            if (shortenNames && isStable(owner)) {
              patch += ctx.addGlobalImport(symbol)
              Term.Name(name)
            } else {
              Term.Select(loop[Term.Ref](owner), Term.Name(name))
            }
          // symbol `a.B#`  becomes `a.B` type
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
        // before: Function2[A, B]
        // after: A => B
        case Type.Apply(functionN(_), args) =>
          val rargs = apply_![Type](args)
          Type.Function(rargs.init, rargs.last)
        // before: Tuple[A, B]
        // after: (A, B)
        case Type.Apply(tupleN(_), args) =>
          val rargs = apply_![Type](args)
          Type.Tuple(rargs)
        // before: HashSet (which resolves to _root_.scala.collection.mutable.HashSet in SemanticdbIndex)
        // after (if shortenNames=false):
        //   _root_.scala.collection.mutable.HashSet
        // after (if shortenNames=true):
        //   import scala.collection.mutable.HashSet
        //   HashSet
        case Name(_) :&&: index.Symbol(sym: Symbol.Global) =>
          val (addImport, t) = symbolToTree(sym)
          patch += addImport
          t
        // before: A.this.B
        // after: A.this.B
        case Type.Select(Term.This(_), _) =>
          tree
        // Given:
        //   val term: com.Bar
        //   val x: term.Tpe = ???
        // avoid `term.com.Bar#Tpe` by not recursing on Tpe.
        case Type.Select(qual, name) =>
          Type.Select(apply_![Term.Ref](qual), name)
        // Given:
        //   val x: A#B = ???
        // don't recurse on B
        case Type.Project(qual, name) =>
          Type.Project(apply_![Type](qual), name)
        case els =>
          super.apply(els)
      }
    }
    val result = transformer.apply(tpe).asInstanceOf[Type]
    result -> patch
  }
}
