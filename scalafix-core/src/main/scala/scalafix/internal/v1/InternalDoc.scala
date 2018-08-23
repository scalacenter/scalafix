package scalafix.internal.v1

import scala.meta.Tree
import scala.meta.contrib.AssociatedComments
import scala.meta.inputs.Input
import scalafix.internal.config.ScalafixConfig
import scalafix.internal.diff.DiffDisable
import scalafix.internal.patch.EscapeHatch
import scalafix.util.MatchingParens
import scalafix.util.TokenList

class InternalDoc(
    val input: Input,
    val tree: LazyValue[Tree],
    val comments: LazyValue[AssociatedComments],
    val config: ScalafixConfig,
    val escapeHatch: LazyValue[EscapeHatch],
    val diffDisable: DiffDisable,
    val matchingParens: LazyValue[MatchingParens],
    val tokenList: LazyValue[TokenList]
)
