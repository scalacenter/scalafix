package scalafix.test

import scalafix.v1.SemanticRule

class ExplicitSynthetic() extends SemanticRule("ExplicitSynthetic") {
  import scalafix.v1._
  import scala.meta._

  override def fix(implicit doc: SemanticDocument): Patch = {
    val patches = doc.tree.collect {
      case Term.Select(_, Term.Name("apply")) =>
        // happens for explicit "List.apply" because Synthetic.symbol returns Some(symbol)
        // for OriginalTree.
        List()
      case infix: Term.ApplyInfix =>
        for {
          synthetic <- infix.syntheticOperators.collect {
            case tappl: TypeApplyTree => tappl
          }
        } yield {
          val targs = synthetic.typeArguments.collect {
            // not a generic solution but sufficient for unit tests
            case ref: TypeRef =>
              Type.Name(ref.symbol.displayName)
          }
          Patch.addRight(
            infix.op,
            targs.mkString("[", ", ", "]")
          )
        }
      case t: Term =>
        for {
          synthetic <- t.synthetics
          sym <- synthetic.symbol
          if sym.displayName == "apply"
        } yield {
          Patch.addRight(t, ".apply")
        }
    }
    patches.flatten.asPatch + Patch.replaceTree(Term.Name("a"), "b")
  }

}
