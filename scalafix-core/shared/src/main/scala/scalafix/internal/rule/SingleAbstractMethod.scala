package scalafix.internal.rule

import scala.meta._
import scalafix.Patch
import scalafix.SemanticdbIndex
import scalafix.rule.RuleCtx
import scalafix.rule.SemanticRule

case class SingleAbstractMethod(index: SemanticdbIndex)
    extends SemanticRule(index, "SingleAbstractMethod") {
  override def description: String = ???
  override def fix(ctx: RuleCtx): Patch = {
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
                    body
                  )
                )
              )
              ) => {
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
      case term @ Defn.Var(
            mods,
            List(Pat.Var(name)),
            _,
            Some(Sam(lambda, tpe))) =>
        println(tpe)
        ctx.replaceTree(
          term,
          Defn
            .Var(mods, List(Pat.Var(name)), Some(tpe), Some(lambda))
            .show[Syntax])

      case term @ Defn.Val(mods, List(Pat.Var(name)), _, Sam(lambda, tpe)) =>
        ctx.replaceTree(
          term,
          Defn.Val(mods, List(Pat.Var(name)), Some(tpe), lambda).show[Syntax])

      case term @ Defn.Def(mods, name, tparams, paramss, _, Sam(lambda, tpe)) =>
        ctx.replaceTree(
          term,
          Defn.Def(mods, name, tparams, paramss, Some(tpe), lambda).show[Syntax]
        )

      case anon @ Sam(lambda, _) =>
        ctx.replaceTree(anon, lambda.show[Syntax])
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
