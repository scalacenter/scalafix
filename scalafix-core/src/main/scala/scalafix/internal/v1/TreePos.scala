package scalafix.internal.v1

import scala.annotation.tailrec
import sourcecode.Text.generate

import scala.meta._

object TreePos {
  private[scalafix] def symbol[T](
      name: Name,
      f: Position => T,
      isEmpty: T => Boolean
  ): T = {
    val syntax = name.syntax
    // workaround for https://github.com/scalameta/scalameta/issues/1083
    if (
      syntax.startsWith("(") &&
      syntax.endsWith(")") &&
      syntax != name.value
    ) {
      val pos =
        Position.Range(name.pos.input, name.pos.start + 1, name.pos.end - 1)
      val t = f(pos)
      if (isEmpty(t)) f(name.pos)
      else t
    } else {
      f(name.pos)
    }
  }

  def symbol(tree: Tree): Position =
    symbolImpl[Position](tree)(identity[Position], _ => false)

  // The implicit function must named `$conforms` to shadow `Predef.$conforms`
  @tailrec
  private[scalafix] def symbolImpl[T](
      tree: Tree
  )(implicit $conforms: Position => T, isEmpty: T => Boolean): T = tree match {
    case name: Name => symbol(name, $conforms, isEmpty)
    case m: Member => symbolImpl(m.name)
    case t: Term.Select => symbolImpl(t.name)
    case t: Term.Interpolate => symbolImpl(t.prefix)
    case t: Term.Apply => symbolImpl(t.fun)
    case t: Term.ApplyInfix => symbolImpl(t.op)
    case t: Term.ApplyUnary => symbolImpl(t.op)
    case t: Term.ApplyType => symbolImpl(t.fun)
    case t: Term.Assign => symbolImpl(t.lhs)
    case t: Term.Ascribe => symbolImpl(t.expr)
    case t: Term.Annotate => symbolImpl(t.expr)
    case t: Term.New => symbolImpl(t.init)
    case t: Type.Select => symbolImpl(t.name)
    case t: Type.Project => symbolImpl(t.name)
    case t: Type.Singleton => symbolImpl(t.ref)
    case t: Type.Apply => symbolImpl(t.tpe)
    case t: Type.ApplyInfix => symbolImpl(t.op)
    case t: Type.Annotate => symbolImpl(t.tpe)
    case t: Type.ByName => symbolImpl(t.tpe)
    case t: Type.Repeated => symbolImpl(t.tpe)
    case t: Pat.Bind => symbolImpl(t.lhs)
    case t: Pat.Extract => symbolImpl(t.fun)
    case t: Pat.ExtractInfix => symbolImpl(t.op)
    case t: Pat.Interpolate => symbolImpl(t.prefix)
    case Defn.Val(_, p :: Nil, _, _) => symbolImpl(p)
    case Decl.Val(_, p :: Nil, _) => symbolImpl(p)
    case Defn.Var(_, p :: Nil, _, _) => symbolImpl(p)
    case Decl.Var(_, p :: Nil, _) => symbolImpl(p)
    case t: Importee.Rename => symbolImpl(t.name)
    case t: Importee.Name => symbolImpl(t.name)
    case Importer(_, i :: Nil) => symbolImpl(i)
    case t: Init => symbolImpl(t.tpe)
    case t: Mod.Annot => symbolImpl(t.init)
    case _ => tree.pos
  }
}
