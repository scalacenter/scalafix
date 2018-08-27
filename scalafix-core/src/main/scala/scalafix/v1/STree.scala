package scalafix.v1

import scala.meta.Tree
import scala.meta.internal.{semanticdb => s}
import scala.runtime.Statics

sealed abstract class STree

case object NoTree extends STree

final class IdTree(
    value: s.IdTree,
    val symbol: Symbol
)(implicit symtab: Symtab)
    extends STree {
  override def toString: String =
    s"IdTree($symbol)"
  override def equals(obj: Any): Boolean =
    this.eq(obj.asInstanceOf[AnyRef]) || (obj match {
      case s: IdTree =>
        this.symbol == s.symbol
      case _ => false
    })
  override def hashCode(): Int = {
    var acc = -889275714
    acc = Statics.mix(acc, Statics.anyHash(symbol))
    Statics.finalizeHash(acc, 1)
  }
}

final class SelectTree(
    value: s.SelectTree,
    val qual: STree,
    val id: IdTree
)(implicit symtab: Symtab)
    extends STree {
  override def toString: String =
    s"SelectTree($qual, $id)"
  override def equals(obj: Any): Boolean =
    this.eq(obj.asInstanceOf[AnyRef]) || (obj match {
      case s: SelectTree =>
        this.qual == s.qual &&
          this.id == s.id
      case _ => false
    })
  override def hashCode(): Int = {
    var acc = -889275714
    acc = Statics.mix(acc, Statics.anyHash(qual))
    acc = Statics.mix(acc, Statics.anyHash(id))
    Statics.finalizeHash(acc, 2)
  }
}

final class ApplyTree(
    value: s.ApplyTree,
    val fn: STree,
    val args: List[STree]
)(implicit symtab: Symtab)
    extends STree {
  override def toString: String =
    s"ApplyTree($fn, $args)"
  override def equals(obj: Any): Boolean =
    this.eq(obj.asInstanceOf[AnyRef]) || (obj match {
      case s: ApplyTree =>
        this.fn == s.fn &&
          this.args == s.args
      case _ => false
    })
  override def hashCode(): Int = {
    var acc = -889275714
    acc = Statics.mix(acc, Statics.anyHash(fn))
    acc = Statics.mix(acc, Statics.anyHash(args))
    Statics.finalizeHash(acc, 2)
  }
}

final class TypeApplyTree(
    value: s.TypeApplyTree,
    val fn: STree,
    val targs: List[SType]
)(implicit symtab: Symtab)
    extends STree {
  override def toString: String =
    s"TypeApplyTree($fn, $targs)"
  override def equals(obj: Any): Boolean =
    this.eq(obj.asInstanceOf[AnyRef]) || (obj match {
      case s: TypeApplyTree =>
        this.fn == s.fn &&
          this.targs == s.targs
      case _ => false
    })
  override def hashCode(): Int = {
    var acc = -889275714
    acc = Statics.mix(acc, Statics.anyHash(fn))
    acc = Statics.mix(acc, Statics.anyHash(targs))
    Statics.finalizeHash(acc, 2)
  }
}

final class FunctionTree(
    value: s.FunctionTree,
    val params: List[IdTree],
    val term: STree
)(implicit symtab: Symtab)
    extends STree {
  override def toString: String =
    s"FunctionTree($params, $term)"
  override def equals(obj: Any): Boolean =
    this.eq(obj.asInstanceOf[AnyRef]) || (obj match {
      case s: FunctionTree =>
        this.params == s.params &&
          this.term == s.term
      case _ => false
    })
  override def hashCode(): Int = {
    var acc = -889275714
    acc = Statics.mix(acc, Statics.anyHash(params))
    acc = Statics.mix(acc, Statics.anyHash(term))
    Statics.finalizeHash(acc, 2)
  }
}

final class LiteralTree(
    value: s.LiteralTree,
    val const: Constant
)(implicit symtab: Symtab)
    extends STree {
  override def toString: String =
    s"IdTree($const)"
  override def equals(obj: Any): Boolean =
    this.eq(obj.asInstanceOf[AnyRef]) || (obj match {
      case s: LiteralTree =>
        this.const == s.const
      case _ => false
    })
  override def hashCode(): Int = {
    var acc = -889275714
    acc = Statics.mix(acc, Statics.anyHash(const))
    Statics.finalizeHash(acc, 1)
  }
}

final class MacroExpansionTree(
    value: s.MacroExpansionTree,
    val expandee: STree,
    val tpe: SType
)(implicit symtab: Symtab)
    extends STree {
  override def toString: String =
    s"MacroExpansionTree($expandee, $tpe)"
  override def equals(obj: Any): Boolean =
    this.eq(obj.asInstanceOf[AnyRef]) || (obj match {
      case s: MacroExpansionTree =>
        this.expandee == s.expandee &&
          this.tpe == s.tpe
      case _ => false
    })
  override def hashCode(): Int = {
    var acc = -889275714
    acc = Statics.mix(acc, Statics.anyHash(expandee))
    acc = Statics.mix(acc, Statics.anyHash(tpe))
    Statics.finalizeHash(acc, 2)
  }
}

final class OriginalTree(
    value: s.OriginalTree,
    val tree: Tree
)(implicit symtab: Symtab)
    extends STree {
  override def toString: String =
    s"OriginalTree($tree)"
  override def equals(obj: Any): Boolean =
    this.eq(obj.asInstanceOf[AnyRef]) || (obj match {
      case s: OriginalTree =>
        this.tree == s.tree
      case _ => false
    })
  override def hashCode(): Int = {
    var acc = -889275714
    acc = Statics.mix(acc, Statics.anyHash(tree))
    Statics.finalizeHash(acc, 1)
  }
}
