package scalafix.internal.v1

import scala.annotation.tailrec
import scala.meta._

object TreePos {
  @tailrec def symbol(tree: Tree): Position = tree match {
    case name @ Name(_) =>
      val syntax = name.syntax
      // workaround for https://github.com/scalameta/scalameta/issues/1083
      if (syntax.startsWith("(") &&
        syntax.endsWith(")") &&
        syntax != name.value) {
        Position.Range(name.pos.input, name.pos.start + 1, name.pos.end - 1)
      } else {
        name.pos
      }
    case m: Member => symbol(m.name)
    case t: Term.Select => symbol(t.name)
    case t: Term.Interpolate => symbol(t.prefix)
    case t: Term.Apply => symbol(t.fun)
    case t: Term.ApplyInfix => symbol(t.op)
    case t: Term.ApplyUnary => symbol(t.op)
    case t: Term.ApplyType => symbol(t.fun)
    case t: Term.Assign => symbol(t.lhs)
    case t: Term.Ascribe => symbol(t.expr)
    case t: Term.Annotate => symbol(t.expr)
    case t: Term.New => symbol(t.init)
    case t: Type.Select => symbol(t.name)
    case t: Type.Project => symbol(t.name)
    case t: Type.Singleton => symbol(t.ref)
    case t: Type.Apply => symbol(t.tpe)
    case t: Type.ApplyInfix => symbol(t.op)
    case t: Type.Annotate => symbol(t.tpe)
    case t: Type.ByName => symbol(t.tpe)
    case t: Type.Repeated => symbol(t.tpe)
    case t: Pat.Bind => symbol(t.lhs)
    case t: Pat.Extract => symbol(t.fun)
    case t: Pat.ExtractInfix => symbol(t.op)
    case t: Pat.Interpolate => symbol(t.prefix)
    case Defn.Val(_, p :: Nil, _, _) => symbol(p)
    case Decl.Val(_, p :: Nil, _) => symbol(p)
    case Defn.Var(_, p :: Nil, _, _) => symbol(p)
    case Decl.Var(_, p :: Nil, _) => symbol(p)
    case t: Importee.Rename => symbol(t.name)
    case t: Importee.Name => symbol(t.name)
    case Importer(_, i :: Nil) => symbol(i)
    case t: Init => symbol(t.tpe)
    case t: Mod.Annot => symbol(t.init)
    case _ => tree.pos
  }
}
