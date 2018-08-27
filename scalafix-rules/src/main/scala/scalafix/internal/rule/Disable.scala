package scalafix.internal.rule

import metaconfig.{Conf, Configured}
import scala.meta._
import scala.meta.transversers.Traverser
import scalafix.internal.v0.InputSynthetic
import scalafix.internal.util.SymbolOps
import scalafix.internal.v0.LegacySemanticRule
import scalafix.v0._

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

  final class DisableSymbolMatcher(symbols: List[DisabledSymbol])(
      implicit index: SemanticdbIndex) {
    def findMatch(symbol: Symbol): Option[DisabledSymbol] =
      symbols.find(_.matches(symbol))

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

  private val safeBlocks = new DisableSymbolMatcher(config.allSafeBlocks)
  private val disabledSymbolInSynthetics =
    new DisableSymbolMatcher(config.ifSynthetic)

  private def createLintMessage(
      symbol: Symbol.Global,
      disabled: DisabledSymbol,
      pos: Position,
      details: String = ""): Diagnostic = {
    val message = disabled.message.getOrElse(
      s"${symbol.signature.name} is disabled$details")

    val id = disabled.id.getOrElse(symbol.signature.name)

    errorCategory
      .copy(id = id)
      .at(message, pos)
  }

  private def checkTree(ctx: RuleCtx): Seq[Diagnostic] = {
    def filterBlockedSymbolsInBlock(
        blockedSymbols: List[DisabledSymbol],
        block: Tree): List[DisabledSymbol] =
      ctx.index.symbol(block) match {
        case Some(symbolBlock: Symbol.Global) =>
          val symbolsInMatchedBlocks =
            config.unlessInside.flatMap(
              u =>
                if (u.safeBlocks.exists(_.matches(symbolBlock))) u.symbols
                else List.empty)
          val res = blockedSymbols.filterNot(symbolsInMatchedBlocks.contains)
          res
        case _ => blockedSymbols
      }

    def skipTermSelect(term: Term): Boolean = term match {
      case _: Term.Name => true
      case Term.Select(q, _) => skipTermSelect(q)
      case _ => false
    }

    def handleName(t: Name, blockedSymbols: List[DisabledSymbol])
      : Either[Diagnostic, List[DisabledSymbol]] = {
      val isBlocked = new DisableSymbolMatcher(blockedSymbols)
      ctx.index.symbol(t) match {
        case Some(isBlocked(s: Symbol.Global, disabled)) =>
          SymbolOps.normalize(s) match {
            case g: Symbol.Global if g.signature.name != "<init>" =>
              Left(createLintMessage(g, disabled, t.pos))
            case _ => Right(blockedSymbols)
          }
        case _ => Right(blockedSymbols)
      }
    }

    new ContextTraverser(config.allDisabledSymbols)({
      case (_: Import, _) => Right(List.empty)
      case (Term.Select(q, name), blockedSymbols) if skipTermSelect(q) =>
        handleName(name, blockedSymbols)
      case (Type.Select(q, name), blockedSymbols) if skipTermSelect(q) =>
        handleName(name, blockedSymbols)
      case (
          Term.Apply(
            Term.Select(block @ safeBlocks(_, _), Term.Name("apply")),
            _
          ),
          blockedSymbols
          ) =>
        Right(filterBlockedSymbolsInBlock(blockedSymbols, block)) // <Block>.apply
      case (Term.Apply(block @ safeBlocks(_, _), _), blockedSymbols) =>
        Right(filterBlockedSymbolsInBlock(blockedSymbols, block)) // <Block>(...)
      case (_: Defn.Def, _) =>
        Right(config.allDisabledSymbols) // reset blocked symbols in def
      case (_: Term.Function, _) =>
        Right(config.allDisabledSymbols) // reset blocked symbols in (...) => (...)
      case (t: Name, blockedSymbols) =>
        handleName(t, blockedSymbols)
    }).result(ctx.tree)
  }

  private def checkSynthetics(ctx: RuleCtx): Seq[Diagnostic] = {
    for {
      synthetic <- ctx.index.synthetics.view
      ResolvedName(
        pos,
        disabledSymbolInSynthetics(symbol @ Symbol.Global(_, _), disabled),
        false
      ) <- synthetic.names
    } yield {
      val (details, caret) = pos.input match {
        case synth @ Input.Stream(InputSynthetic(_, input, start, end), _) =>
          // For synthetics the caret should point to the original position
          // but display the inferred code.
          s" and it got inferred as `${synth.text}`" ->
            Position.Range(input, start, end)
        case _ =>
          "" -> pos
      }
      createLintMessage(symbol, disabled, caret, details)
    }
  }

  override def check(ctx: RuleCtx): Seq[Diagnostic] = {
    checkTree(ctx) ++ checkSynthetics(ctx)
  }
}

class DisableLegacy()
    extends LegacySemanticRule(
      "Disable",
      index => Disable(index, DisableConfig.default))
