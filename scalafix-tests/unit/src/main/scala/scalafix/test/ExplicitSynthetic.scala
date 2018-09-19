package scalafix.test

import metaconfig.Configured
import scalafix.v1.SemanticRule

class ExplicitSynthetic(insertInfixTypeParam: Boolean)
    extends SemanticRule("ExplicitSynthetic") {
  import scalafix.v1._
  import scala.meta._

  def this() = this(insertInfixTypeParam = true)

  override def withConfiguration(config: Configuration): Configured[Rule] =
    Configured.ok(
      new ExplicitSynthetic(
        // There is a Scala parser bug in 2.11 and below under -Yrangepos
        // that causes a crash for explicit type parameters on infix operators.
        insertInfixTypeParam = !config.scalaVersion.startsWith("2.11"))
    )

  override def fix(implicit doc: SemanticDocument): Patch = {
    val patches = doc.tree.collect {
      case Term.Select(_, Term.Name("apply")) =>
        // happens for explicit "List.apply" because Synthetic.symbol returns Some(symbol)
        // for OriginalTree.
        None
      case infix: Term.ApplyInfix if insertInfixTypeParam =>
        for {
          synthetic <- infix.syntheticOperator.collect {
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
          synthetic <- t.synthetic
          sym <- synthetic.symbol
          if sym.displayName == "apply"
        } yield {
          Patch.addRight(t, ".apply")
        }
    }
    patches.flatten.asPatch + Patch.replaceTree(q"a", "b")
  }

}
