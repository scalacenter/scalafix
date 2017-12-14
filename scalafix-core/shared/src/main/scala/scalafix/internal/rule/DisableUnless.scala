package scalafix.internal.rule

import metaconfig.{Conf, Configured}

import scala.meta._
import scala.meta.transversers.{Transformer, Traverser}
import scalafix._
import scalafix.internal.config.DisableUnlessConfig
import scalafix.lint.LintCategory
import scalafix.rule.RuleCtx
import scalafix.util.SymbolMatcher

final case class DisableUnless(index: SemanticdbIndex,
                               config: DisableUnlessConfig)
    extends SemanticRule(index, "DisableUnless") {

  private lazy val errorCategory: LintCategory =
    LintCategory.error(
      """Some constructs are unsafe to use and should be avoided""".stripMargin
    )

  override def init(config: Conf): Configured[Rule] =
    config
      .getOrElse("disableUnless", "DisableUnless")(DisableUnlessConfig.default)
      .map(DisableUnless(index, _))

  private lazy val disabledSymbol: SymbolMatcher =
    SymbolMatcher.normalized(config.allSymbols: _*)
  private lazy val disabledBlock: SymbolMatcher =
    SymbolMatcher.normalized(config.allBlocks: _*)

  override def check(ctx: RuleCtx): Seq[LintMessage] = {
    def search[T](tree: Tree)(fn: PartialFunction[Tree, Option[T]]): List[T] = {
      val buf = scala.collection.mutable.ListBuffer[T]()
      object traverser extends Traverser {
        override def apply(tree: Tree): Unit = {
          if (fn.isDefinedAt(tree)) {
            fn(tree).foreach(buf.+=)
          } else {
            super.apply(tree)
          }
        }
      }
      traverser(tree)
      buf.toList
    }

    search(ctx.tree) {
      case Term.Apply(Term.Select(disabledBlock(_), Term.Name("apply")), _) =>
        None
      case Term.Apply(disabledBlock(_), _) =>
        None
      case disabledSymbol(t) =>
        val Some(symbol @ Symbol.Global(_, signature)) = ctx.index.symbol(t)
        val message = config
          .customMessage(symbol)
          .getOrElse(s"${signature.name} is disabled")
        Some(
          errorCategory
            .copy(id = signature.name)
            .at(message, t.pos)
        )
    }
  }
}
