package scalafix.internal.rule

import scala.meta._
import scalafix.Patch
import scalafix.SemanticdbIndex
import scalafix.rule.RuleCtx
import scalafix.rule.SemanticRule

import scala.collection.mutable

// SAM conversion: http://www.scala-lang.org/files/archive/spec/2.12/06-expressions.html#sam-conversion
case class SingleAbstractMethod(index: SemanticdbIndex)
    extends SemanticRule(index, "SingleAbstractMethod") {
  override def description: String = ???
  override def fix(ctx: RuleCtx): Patch = {
    val visited = mutable.Set.empty[Tree]
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
              ) if !visited.contains(tree) =>

            println(s"visited: $tree")
            visited += tree

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

    ctx.tree.collect {
      case term @ Defn.Val(mods, List(Pat.Var(name)), tpe0, Sam(lambda, tpe)) =>
        println(name)
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
        println(name)
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
        println(name)
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
        println("\\x")
        ctx.replaceTree(
          tree,
          lambda.syntax
        )

    }.asPatch
  }
}
