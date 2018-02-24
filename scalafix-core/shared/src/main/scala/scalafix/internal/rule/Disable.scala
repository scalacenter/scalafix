package scalafix.internal.rule

import metaconfig.{Conf, Configured}

import scala.meta._
import scala.meta.transversers.Traverser
import scalafix.internal.config.{DisableConfig, DisabledSymbol}
import scalafix.internal.util.SymbolOps
import scalafix.lint.{LintCategory, LintMessage}
import scalafix.rule.{Rule, RuleCtx, SemanticRule}
import scalafix.util.SemanticdbIndex

object Disable {

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

  def matchDisabledSymbol(disabledSymbol: DisabledSymbol, symbol: Symbol)(
      implicit index: SemanticdbIndex): Boolean =
    disabledSymbol match {
      case DisabledSymbol(Some(s), _, _, banHierarchy, _) =>
        if (banHierarchy) {
          println(symbol)
          println(index.denotation(symbol).map(_.overrides))
        }
        SymbolOps.isSameNormalized(symbol, s) || (
          banHierarchy &&
            index
              .denotation(symbol)
              .exists(_.overrides.exists(SymbolOps.isSameNormalized(symbol, _)))
        )
      case d @ DisabledSymbol(None, _, _, _, Some(_)) =>
        d.compiledRegex.exists(_.findFirstIn(symbol.toString).isDefined)
      case DisabledSymbol(None, _, _, _, None) =>
        true // weird case, but it has some logic
    }

  final class DisableSymbolMatcher(symbols: List[DisabledSymbol])(
      implicit index: SemanticdbIndex) {
    def findMatch(symbol: Symbol): Option[DisabledSymbol] =
      symbols.find(matchDisabledSymbol(_, symbol))

    def unapply(tree: Tree): Option[(Tree, DisabledSymbol)] =
      index
        .symbol(tree)
        .flatMap(findMatch(_).map(ds => (tree, ds)))

    def unapply(symbol: Symbol): Option[(Symbol, DisabledSymbol)] =
      findMatch(symbol).map(ds => (symbol, ds))
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

  override def init(config: Conf): Configured[Rule] =
    config
      .getOrElse("disable", "Disable")(DisableConfig.default)
      .map(Disable(index, _))

  private val safeBlock = new DisableSymbolMatcher(config.allSafeBlocks)
  private val disabledSymbolInSynthetics =
    new DisableSymbolMatcher(config.ifSynthetic)

  private def createLintMessage(
      symbol: Symbol.Global,
      disabled: DisabledSymbol,
      pos: Position,
      details: String = ""): LintMessage = {
    val message = disabled.message.getOrElse(
      s"${symbol.signature.name} is disabled$details")

    val id = disabled.id.getOrElse(symbol.signature.name)

    errorCategory
      .copy(id = id)
      .at(message, pos)
  }

  private def checkTree(ctx: RuleCtx): Seq[LintMessage] = {
    def filterBlockedSymbolsInBlock(
        blockedSymbols: List[DisabledSymbol],
        block: Tree): List[DisabledSymbol] =
      ctx.index.symbol(block) match {
        case Some(symbolBlock: Symbol.Global) =>
          val symbolsInMatchedBlocks =
            config.unlessInside.flatMap(
              u =>
                if (matchDisabledSymbol(u.safeBlock, symbolBlock)) u.symbols
                else List.empty)
          val res = blockedSymbols.filterNot(symbolsInMatchedBlocks.contains)
          res
        case _ => blockedSymbols
      }

    new ContextTraverser(config.allDisabledSymbols)({
      case (_: Import, _) => Right(List.empty)
      case (
          Term
            .Apply(Term.Select(block @ safeBlock(_, _), Term.Name("apply")), _),
          blockedSymbols) =>
        Right(filterBlockedSymbolsInBlock(blockedSymbols, block)) // <Block>.apply
      case (Term.Apply(block @ safeBlock(_, _), _), blockedSymbols) =>
        Right(filterBlockedSymbolsInBlock(blockedSymbols, block)) // <Block>(...)
      case (_: Defn.Def, _) =>
        Right(config.allDisabledSymbols) // reset blocked symbols in def
      case (_: Term.Function, _) =>
        Right(config.allDisabledSymbols) // reset blocked symbols in (...) => (...)
      case (t: Name, blockedSymbols) =>
        val isBlocked = new DisableSymbolMatcher(blockedSymbols)
        ctx.index.symbol(t) match {
          case Some(isBlocked(s: Symbol.Global, disabled)) =>
            Left(
              createLintMessage(s, disabled, t.pos)
            )
          case _ => Right(blockedSymbols)
        }
    }).result(ctx.tree)
  }

  private def checkSynthetics(ctx: RuleCtx): Seq[LintMessage] = {
    for {
      document <- ctx.index.documents.view
      ResolvedName(
        pos,
        disabledSymbolInSynthetics(symbol @ Symbol.Global(_, _), disabled),
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
      createLintMessage(symbol, disabled, caret, details)
    }
  }

  override def check(ctx: RuleCtx): Seq[LintMessage] = {
    checkTree(ctx) ++ checkSynthetics(ctx)
  }
}
