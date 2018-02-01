package scalafix.internal.rule

import scala.meta._
import scalafix.Patch
import scalafix.SemanticdbIndex
import scalafix.rule.RuleCtx
import scalafix.rule.SemanticRule

/*
http://www.scala-lang.org/files/archive/spec/2.12/06-expressions.html#sam-conversion

SAM conversion

An expression `(p1, ..., pN) => body` of function type `(T1, ..., TN) => T`
  is sam-convertible to the expected type S if the following holds:

 * the class C of S declares an abstract method m with signature (p1: A1, ..., pN: AN): R;
 * besides m, C must not declare or inherit any other deferred value members;
 * the method m must have a single argument list;
 * there must be a type U that is a subtype of S, so that the expression
  new U { final def m(p1: A1, ..., pN: AN): R = body } is well-typed
  (conforming to the expected type S);
 * for the purpose of scoping, m should be considered a static member
  (U's members are not in scope in body);
 * (A1, ..., AN) => R is a subtype of (T1, ..., TN) => T
  (satisfying this condition drives type inference of unknown type parameters in S);

Note that a function literal that targets a SAM is not necessarily compiled to the above
instance creation expression. This is platform-dependent.

It follows that:

 * if class C defines a constructor, it must be accessible and must define exactly one, empty,
  argument list;
 * class C cannot be final or sealed
  (for simplicity we ignore the possibility of SAM conversion in the same compilation
   unit as the sealed class);
 * m cannot be polymorphic;
 * it must be possible to derive a fully-defined type U from S by inferring any unknown
  type parameters of C.

Finally, we impose some implementation restrictions (these may be lifted in future releases):

 * C must not be nested or local (it must not capture its environment, as that results
  in a zero-argument constructor)
 * C's constructor must not have an implicit argument list (this simplifies type inference);
 * C must not declare a self type (this simplifies type inference);
 * C must not be @specialized.

 */
case class SingleAbstractMethod(index: SemanticdbIndex)
    extends SemanticRule(index, "SingleAbstractMethod") {
  override def description: String = ???
  override def fix(ctx: RuleCtx): Patch = {

    // extract the top name position java.util.TimerTask => TimerTask
    def parentNamePosition(tpe: Type): Option[Position] = {
      tpe match {
        case Type.Select(_, name) => Some(name.pos)
        case name: Type.Name => Some(name.pos)
        case _ => None
      }
    }

    object Sam {
      def unapply(tree: Tree): Option[(Term.Function, Type)] = {
        tree match {
          case Term.NewAnonymous(
            Template(
              _,
              List(Init(clazz, _, _)),
              _,
              List(Defn.Def(_, method, _, List(params), _, body))
            )
          ) =>
            val singleAbstractOverride =
              (for {
                definition <- index.denotation(method)
                overrideSymbol <- definition.overrides.headOption if definition.overrides.size == 1
                overrideDefinition <- index.denotation(overrideSymbol)
              } yield overrideDefinition.isAbstract).getOrElse(false)

            val singleMember =
              (for {
                position <- parentNamePosition(clazz)
                symbol <- index.symbol(position)
                denfinition <- index.denotation(symbol)
              } yield denfinition.members.size == 1).getOrElse(false)

            if (singleAbstractOverride && singleMember) {
               Some(
                 (
                   Term.Function(params.map(_.copy(decltpe = None)), body),
                   clazz
                 )
               )
            } else {
              None
            }

          case _ => None
        }
      }
    }

    collectOnce(ctx.tree) {
      case term @ Defn.Val(mods, List(Pat.Var(name)), tpe0, Sam(lambda, tpe)) =>
        ctx.replaceTree(
          term,
          Defn.Val(mods, List(Pat.Var(name)), Some(tpe0.getOrElse(tpe)), lambda).show[Syntax]
        )
      case term @ Defn.Var(mods, List(Pat.Var(name)), tpe0, Some(Sam(lambda, tpe))) =>
        ctx.replaceTree(
          term,
          Defn.Var(mods, List(Pat.Var(name)), Some(tpe0.getOrElse(tpe)), Some(lambda)).show[Syntax]
        )
      case term @ Defn.Def(mods, name, tparams, paramss, decltpe, Sam(lambda, tpe)) =>
        ctx.replaceTree(
          term,
          Defn.Def(mods, name, tparams, paramss, Some(decltpe.getOrElse(tpe)), lambda).show[Syntax]
        )

      case tree @ Sam(lambda, _) =>
        ctx.replaceTree(
          tree ,
          lambda.show[Syntax]
        )

    }.asPatch
  }

  private def collectOnce[T](tree: Tree)(
      fn: PartialFunction[Tree, T]): List[T] = {
    val liftedFn = fn.lift
    val buf = scala.collection.mutable.ListBuffer[T]()
    object traverser extends Traverser {
      override def apply(tree: Tree): Unit = {
        liftedFn(tree) match {
          case Some(t) => buf += t
          case None => super.apply(tree)
        }
      }
    }
    traverser(tree)
    buf.toList
  }
}
