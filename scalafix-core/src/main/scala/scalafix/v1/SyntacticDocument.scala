package scalafix.v1

import scala.meta.Dialect
import scala.meta.Input
import scala.meta.Source
import scala.meta.Tokens
import scala.meta.Tree
import scala.meta.contrib.AssociatedComments
import scala.meta.parsers.Parsed

import scalafix.internal.config.ScalaVersion
import scalafix.internal.config.ScalafixConfig
import scalafix.internal.diff.DiffDisable
import scalafix.internal.patch.EscapeHatch
import scalafix.internal.v1.InternalDoc
import scalafix.internal.v1.LazyValue
import scalafix.util.MatchingParens
import scalafix.util.TokenList

final class SyntacticDocument private[scalafix] (
    private[scalafix] val internal: InternalDoc
) {
  def input: Input = internal.input
  def tree: Tree = internal.tree.value
  def tokens: Tokens = tree.tokens
  def comments: AssociatedComments = internal.comments.value
  def matchingParens: MatchingParens = internal.matchingParens.value
  def tokenList: TokenList = internal.tokenList.value
  override def toString: String = s"SyntacticDocument(${input.syntax})"
}

object SyntacticDocument {
  @deprecated(
    "use fromInput(input: Input, scalaVersion: ScalaVersion) instead",
    "0.9.28"
  )
  def fromInput(input: Input): SyntacticDocument = {
    fromInput(input, ScalaVersion.scala2)
  }
  @deprecated(
    "use fromInput(input: Input, scalaVersion: ScalaVersion) instead",
    "0.9.28"
  )
  def fromInput(inout: Input, dialect: Dialect): SyntacticDocument = {
    val tree = parse(inout, dialect).get: Tree
    fromTree(tree)
  }

  def fromInput(input: Input, scalaVersion: ScalaVersion): SyntacticDocument = {
    SyntacticDocument(
      input,
      DiffDisable.empty,
      ScalafixConfig.default.copy(scalaVersion = scalaVersion)
    )
  }

  def fromTree(tree: Tree): SyntacticDocument = {
    SyntacticDocument(
      tree.pos.input,
      LazyValue.now(tree),
      DiffDisable.empty,
      ScalafixConfig.default
    )
  }

  private def parse(input: Input, config: ScalafixConfig): Parsed[Source] = {
    import scala.meta._
    val dialect = config.dialectForFile(input.syntax)
    dialect(input).parse[Source]
  }

  private def parse(input: Input, dialect: Dialect): Parsed[Source] = {
    import scala.meta._
    dialect(input).parse[Source]
  }

  private[scalafix] def apply(
      input: Input,
      diffDisable: DiffDisable,
      config: ScalafixConfig
  ): SyntacticDocument = {
    val tree = LazyValue.later { () =>
      parse(input, config).get: Tree
    }
    apply(input, tree, diffDisable, config)
  }

  private[scalafix] def apply(
      input: Input,
      tree: LazyValue[Tree],
      diffDisable: DiffDisable,
      config: ScalafixConfig
  ): SyntacticDocument = {
    val tokens = LazyValue.later { () =>
      tree.value.tokens
    }
    val comments = LazyValue.later { () =>
      AssociatedComments(tree.value)
    }
    val escape = LazyValue.later { () =>
      EscapeHatch(input, tree, comments, diffDisable)
    }
    val matchingParens = LazyValue.later { () =>
      MatchingParens(tokens.value)
    }
    val tokenList = LazyValue.later { () =>
      TokenList(tokens.value)
    }
    val internal = new InternalDoc(
      input = input,
      tree = tree,
      comments = comments,
      config = config,
      escapeHatch = escape,
      matchingParens = matchingParens,
      tokenList = tokenList
    )
    new SyntacticDocument(internal)
  }
}
