package scalafix.internal.rule

import metaconfig.{Conf, Configured}

import scala.meta._
import scala.meta.transversers.Traverser
import scalafix._
import scalafix.internal.config.DisableUnlessConfig
import scalafix.internal.util.SymbolOps
import scalafix.lint.LintCategory
import scalafix.rule.RuleCtx
import scalafix.util.SymbolMatcher

object DisableUnless {

  /**
    * Searches for specific nodes in Tree and adds result of searching to the list.
    * Also passes context through tree traversal.
    * fn can either return a value. that stops the searching, or change the context.
    */
  class SearcherWithContext[T, U](initContext: U)(
      fn: PartialFunction[(Tree, U), Either[T, U]])
      extends Traverser {
    private var context: U = initContext
    private val buf = scala.collection.mutable.ListBuffer[T]()
    // it can be immutable...

    private val liftedFn = fn.lift

    override protected def apply(tree: Tree): Unit = {
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

    def result(tree: Tree): List[T] = {
      context = initContext
      buf.clear()
      apply(tree)
      buf.toList
    }
  }
}

final case class DisableUnless(
    index: SemanticdbIndex,
    config: DisableUnlessConfig)
    extends SemanticRule(index, "DisableUnless") {

  import DisableUnless._

  private lazy val errorCategory: LintCategory =
    LintCategory.error(
      """Some constructs are unsafe to use and should be avoided""".stripMargin
    )

  override def init(config: Conf): Configured[Rule] =
    config
      .getOrElse("disableUnless", "DisableUnless")(DisableUnlessConfig.default)
      .map(DisableUnless(index, _))

  private lazy val disabledBlock: SymbolMatcher =
    SymbolMatcher.normalized(config.allBlocks: _*)

  override def check(ctx: RuleCtx): Seq[LintMessage] = {
    def filterBlockedSymbolsInBlock(
        blockedSymbols: List[Symbol.Global],
        block: Tree): List[Symbol.Global] = {
      val Some(symbolBlock: Symbol.Global) = ctx.index.symbol(block)
      blockedSymbols.filter(sym =>
        !config.symbolsInBlock(symbolBlock).contains(sym))
    }

    def treeIsBlocked(
        tree: Tree,
        blockedSymbols: List[Symbol.Global]): Boolean =
      ctx.index.symbol(tree) match {
        case Some(s: Symbol.Global) =>
          blockedSymbols.exists(SymbolOps.isSameNormalized(_, s))
        case _ => false
      }

    new SearcherWithContext(config.allSymbols)({
      case (
          Term.Apply(Term.Select(disabledBlock(block), Term.Name("apply")), _),
          blockedSymbols) =>
        Right(filterBlockedSymbolsInBlock(blockedSymbols, block)) // <Block>.apply
      case (Term.Apply(disabledBlock(block), _), blockedSymbols) =>
        Right(filterBlockedSymbolsInBlock(blockedSymbols, block)) // <Block>(...)
      case (_: Defn.Def, _) =>
        Right(config.allSymbols) // reset blocked symbols in def
      case (_: Term.Function, _) =>
        Right(config.allSymbols) // reset blocked symbols in (...) => (...)
      case (t, blockedSymbols) if treeIsBlocked(t, blockedSymbols) =>
        val Some(symbol @ Symbol.Global(_, signature)) = ctx.index.symbol(t)
        val message = config
          .customMessage(symbol)
          .getOrElse(s"${signature.name} is disabled")

        Left(
          errorCategory
            .copy(id = signature.name)
            .at(message, t.pos)
        )
    }).result(ctx.tree)
  }
}
