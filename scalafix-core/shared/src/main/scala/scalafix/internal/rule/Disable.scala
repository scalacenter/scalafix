package scalafix.internal.rule

import metaconfig.{Conf, Configured}

import scala.meta._
import scala.meta.transversers.Traverser
import scalafix.config.CustomMessage
import scalafix.internal.config.DisableConfig
import scalafix.internal.util.SymbolOps
import scalafix.lint.{LintCategory, LintMessage}
import scalafix.rule.{Rule, RuleCtx, SemanticRule}
import scalafix.util.{SemanticdbIndex, SymbolMatcher}

object Disable {

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

    def result(tree: Tree): List[T] = {
      context = initContext
      buf.clear()
      apply(tree)
      buf.toList
    }
  }
}

final case class Disable(index: SemanticdbIndex, config: DisableConfig)
    extends SemanticRule(index, "Disable") {

  import Disable._

  private lazy val errorCategory: LintCategory =
    LintCategory.error(
      """Some constructs are unsafe to use and should be avoided""".stripMargin
    )

  override def description: String =
    "Linter that reports an error on a configurable set of symbols."

  private lazy val disabledSymbolInSynthetics: SymbolMatcher =
    SymbolMatcher.normalized(config.allSymbolsInSynthetics: _*)

  private lazy val disabledBlock: SymbolMatcher =
    SymbolMatcher.normalized(config.allUnlessBlocks: _*)

  override def init(config: Conf): Configured[Rule] =
    config
      .getOrElse("disable", "Disable")(DisableConfig.default)
      .map(Disable(index, _))

  private def createLintMessage(
      symbol: Symbol.Global,
      signature: Signature,
      custom: Option[CustomMessage[Symbol.Global]],
      pos: Position,
      details: String = ""): LintMessage = {
    val message = config
      .customMessage(symbol)
      .flatMap(_.message)
      .getOrElse(s"${signature.name} is disabled$details")

    val id = custom
      .flatMap(_.id)
      .getOrElse(signature.name)

    errorCategory
      .copy(id = id)
      .at(message, pos)
  }

  private def checkTree(ctx: RuleCtx): Seq[LintMessage] = {
    def filterBlockedSymbolsInBlock(
        blockedSymbols: List[Symbol.Global],
        block: Tree): List[Symbol.Global] = {
      val Some(symbolBlock: Symbol.Global) = ctx.index.symbol(block)
      blockedSymbols.filter(sym =>
        !config.symbolsInUnlessBlock(symbolBlock).contains(sym))
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
      case (_: Import, _) => Right(List.empty)
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
        Left(
          createLintMessage(
            symbol,
            signature,
            config.customMessage(symbol),
            t.pos)
        )
    }).result(ctx.tree)
  }

  private def checkSynthetics(ctx: RuleCtx): Seq[LintMessage] = {
    for {
      document <- ctx.index.documents.view
      ResolvedName(
        pos,
        disabledSymbolInSynthetics(symbol @ Symbol.Global(_, signature)),
        false
      ) <- document.synthetics.view.flatMap(_.names)
    } yield {
      val (details, caret) = pos.input match {
        case synthetic @ Input.Synthetic(_, input, start, end) =>
          // For synthetics the caret should point to the original position
          // but display the inferred code.
          s" and it got inferred as `${synthetic.text}`" ->
            Position.Range(input, start, end)
        case _ =>
          "" -> pos
      }
      createLintMessage(
        symbol,
        signature,
        config.customMessage(symbol),
        caret,
        details)
    }
  }

  override def check(ctx: RuleCtx): Seq[LintMessage] = {
    checkTree(ctx) ++ checkSynthetics(ctx)
  }
}
