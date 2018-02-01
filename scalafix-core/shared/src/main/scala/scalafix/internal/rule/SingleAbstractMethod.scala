package scalafix.internal.rule

import scala.meta._
import scalafix.Patch
import scalafix.SemanticdbIndex
import scalafix.rule.RuleCtx
import scalafix.rule.SemanticRule

// SAM conversion: http://www.scala-lang.org/files/archive/spec/2.12/06-expressions.html#sam-conversion
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
                List(Init(clazz, _, _)),
                _,
                List(Defn.Def(_, method, _, List(params), _, body))
              )
              ) =>
            val singleAbstractOverride =
              (for {
                definition <- index.denotation(method)
                overrideSymbol <- definition.overrides.headOption
                if definition.overrides.size == 1
                overrideDefinition <- index.denotation(overrideSymbol)
              } yield overrideDefinition.isAbstract).getOrElse(false)

            val singleMember =
              (for {
                symbol <- index.symbol(clazz)
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
          Defn
            .Val(mods, List(Pat.Var(name)), Some(tpe0.getOrElse(tpe)), lambda)
            .syntax
        )
      case term @ Defn.Var(
            mods,
            List(Pat.Var(name)),
            tpe0,
            Some(Sam(lambda, tpe))) =>
        ctx.replaceTree(
          term,
          Defn
            .Var(
              mods,
              List(Pat.Var(name)),
              Some(tpe0.getOrElse(tpe)),
              Some(lambda))
            .syntax
        )
      case term @ Defn.Def(
            mods,
            name,
            tparams,
            paramss,
            decltpe,
            Sam(lambda, tpe)) =>
        ctx.replaceTree(
          term,
          Defn
            .Def(
              mods,
              name,
              tparams,
              paramss,
              Some(decltpe.getOrElse(tpe)),
              lambda)
            .syntax
        )

      case tree @ Sam(lambda, _) =>
        ctx.replaceTree(
          tree,
          lambda.syntax
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
