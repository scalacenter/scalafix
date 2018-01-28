package scalafix.internal.util

import scala.meta.Tree
import scala.meta.transversers.Traverser

/**
  * A tree traverser to collect values with a custom context.
  * At every tree node, either builds a new Context or returns a new Value to accumulate.
  * To collect all accumulated values, use result(Tree).
  */
class ContextTraverser[Value, Context](initContext: Context)(
    fn: PartialFunction[(Tree, Context), Either[Value, Context]])
    extends Traverser {
  private var context: Context = initContext
  private val buf = scala.collection.mutable.ListBuffer[Value]()

  private val liftedFn = fn.lift

  override def apply(tree: Tree): Unit = {
    liftedFn((tree, context)) match {
      case Some(Left(res)) =>
        buf += res
      case Some(Right(newContext)) =>
        val oldContext = context
        context = newContext
        super.apply(tree)
        context = oldContext
      case None =>
        super.apply(tree)
    }
  }

  def result(tree: Tree): List[Value] = {
    context = initContext
    buf.clear()
    apply(tree)
    buf.toList
  }
}
