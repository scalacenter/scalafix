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
      ctx.index.symbol(block) match {
        case Some(symbolBlock: Symbol.Global) =>
          blockedSymbols.filter(sym =>
            !config.symbolsInSafeBlock(symbolBlock).contains(sym))
        case _ => blockedSymbols
      }
    }

    new ContextTraverser(config.allSymbols)({
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
      case (t: Name, blockedSymbols) => {
        val isBlocked = SymbolMatcher.normalized(blockedSymbols: _*)
        ctx.index.symbol(t) match {
          case Some(isBlocked(s: Symbol.Global)) =>
            Left(
              createLintMessage(s, s.signature, config.customMessage(s), t.pos)
            )
          case _ => Right(blockedSymbols)
        }
      }
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
