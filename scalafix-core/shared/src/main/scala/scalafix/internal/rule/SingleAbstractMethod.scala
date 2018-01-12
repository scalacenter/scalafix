package scalafix.internal.rule

// import scala.meta.internal.semanticdb._
// import scala.meta.interactive.InteractiveSemanticdb

import scalafix.syntax._

import scala.meta._
import scalafix.Patch
import scalafix.SemanticdbIndex
import scalafix.rule.RuleCtx
import scalafix.rule.SemanticRule

// import scala.tools.nsc.interactive.Response

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

    println(ctx.index.database)

    object Sam {
      def unapply(tree: Tree): Option[(Term.Function, Type)] = {
        tree match {
          case Term.NewAnonymous(
              Template(
                _,
                List(Init(tpe @ Type.Name(className), _, _)),
                _,
                List(
                  Defn.Def(
                    _,
                    Term.Name(methodName),
                    _,
                    List(params),
                    _,
                    body)))) => {

              // println(tpe.pos.start)
              // println(tpe.pos.end)
              // val d = ctx.index.denotation(tpe).get
              // println(d.names)
              // println(d)
              // println(d.flags)
              // println(d.name)
              // println(d.signature)
              // d.names.foreach(println)
              // d.members.foreach(println)
            
              Some(
                (
                  Term.Function(params.map(_.copy(decltpe = None)), body),
                  tpe
                )
              )
          }
          case _ => None
        }
      }
    }

    collectOnce(ctx.tree) {
      case term @ Defn.Val(mods, List(Pat.Var(name)), _, Sam(lambda, tpe)) =>
        ctx.replaceTree(
          term,
          Defn.Val(mods, List(Pat.Var(name)), Some(tpe), lambda).show[Syntax])

      case in @ init"TimerTask" => {
        in.denotation.get.members.foreach(m =>
          println(m.isAbstract)
        )
        Patch.empty
      }

      // case t: Defn.Class

      // case v: Defn.Val =>
      //   println(v.show[Syntax])
      //   val d = ctx.index.denotation(v.pats.head).get
      //   val s2 = ctx.index.symbol(d.names.head.position).get
      //   val d2 = ctx.index.denotation(s2).get
      //   println(d2)
        


      //   Patch.empty
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
