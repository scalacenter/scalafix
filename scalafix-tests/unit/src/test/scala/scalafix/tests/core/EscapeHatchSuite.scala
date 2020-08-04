package scalafix.tests.core

import java.nio.file.Paths

import org.scalatest.funsuite.AnyFunSuite
import scalafix.internal.config.ScalafixConfig
import scalafix.internal.diff.{DiffDisable, EmptyDiff, NewFile}
import scalafix.internal.patch.EscapeHatch
import scalafix.internal.v0.LegacyRuleCtx
import scalafix.internal.v1.LazyValue
import scalafix.patch.Patch
import scalafix.rule.RuleName
import scalafix.v0.SemanticdbIndex
import scalafix.v1.SyntacticDocument

import scala.meta.{Source, Tree}
import scala.meta.contrib.AssociatedComments
import scala.meta.inputs.Input

class EscapeHatchSuite extends AnyFunSuite {

  private val noEscapes = Input.String(
    """object Foo"""
  )
  private val scalafixOk = Input.String(
    """
      |object Foo // scalafix:ok
    """.stripMargin
  )
  private val scalafixOnOff = Input.String(
    """
      |// scalafix:off
      |object Foo
      |// scalafix:on
    """.stripMargin
  )
  private val suppressWarnings = Input.String(
    """
      |@SuppressWarnings(Array("all"))
      |object Foo
    """.stripMargin
  )

  private class DontTouchMe(who: String) extends RuntimeException
  private lazy val untouchableTree: LazyValue[Tree] =
    LazyValue.later(() => throw new DontTouchMe("tree"))
  private lazy val untouchableComments: LazyValue[AssociatedComments] =
    LazyValue.later(() => throw new DontTouchMe("associated comments"))

  test(
    "`apply` should not evaluate tree nor associated comments if escapes are not found"
  ) {
    EscapeHatch(noEscapes, untouchableTree, untouchableComments, EmptyDiff)
  }

  test(
    "`apply` should evaluate tree and associated comments if 'scalafix:ok' is found"
  ) {
    val (input, tree, comments) = params(scalafixOk)

    intercept[DontTouchMe] {
      EscapeHatch(input, untouchableTree, comments, EmptyDiff)
    }
    intercept[DontTouchMe] {
      EscapeHatch(input, tree, untouchableComments, EmptyDiff)
    }
  }

  test("`apply` should evaluate tree if 'scalafix:on|off' is found") {
    val (input, tree, comments) = params(scalafixOnOff)

    intercept[DontTouchMe] {
      EscapeHatch(input, untouchableTree, comments, EmptyDiff)
    }
    EscapeHatch(input, tree, untouchableComments, EmptyDiff) // should not touch comments
  }

  test("`apply` should evaluate tree if `@SuppressWarnings` is found") {
    val (input, tree, comments) = params(suppressWarnings)

    intercept[DontTouchMe] {
      EscapeHatch(input, untouchableTree, comments, EmptyDiff)
    }
    EscapeHatch(input, tree, untouchableComments, EmptyDiff) // should not touch comments
  }

  test(
    "`isEmpty` should not evaluate tree nor associated comments if escapes are not found"
  ) {
    assert(
      EscapeHatch(noEscapes, untouchableTree, untouchableComments, EmptyDiff).isEmpty
    )
  }

  test(
    "`filter` should not evaluate tree nor associated comments if no escapes are found"
  ) {
    val input = noEscapes
    val doc = SyntacticDocument(
      input,
      untouchableTree,
      EmptyDiff,
      ScalafixConfig.default
    )
    implicit val ctx = new LegacyRuleCtx(doc)
    implicit val idx = SemanticdbIndex.empty
    val patches = Map(RuleName("foo") -> Patch.empty)
    val hatch =
      EscapeHatch(input, untouchableTree, untouchableComments, EmptyDiff)

    assert(hatch.filter(patches) == (List(Patch.empty), Nil))
  }

  test("should be empty if file contains no escapes nor git diffs") {
    val (input, tree, comments) = params(noEscapes)
    val hatch = EscapeHatch(input, tree, comments, EmptyDiff)

    assert(hatch.isEmpty)
  }

  test("should not be empty if file contains 'scalafix:ok'") {
    val (input, tree, comments) = params(scalafixOk)
    val hatch = EscapeHatch(input, tree, comments, EmptyDiff)

    assert(!hatch.isEmpty)
  }

  test("should not be empty if file contains 'scalafix:on|off'") {
    val (input, tree, comments) = params(scalafixOnOff)
    val hatch = EscapeHatch(input, tree, comments, EmptyDiff)

    assert(!hatch.isEmpty)
  }

  test("should not be empty if file contains `@SuppressWarnings`") {
    val (input, tree, comments) = params(suppressWarnings)
    val hatch = EscapeHatch(input, tree, comments, EmptyDiff)

    assert(!hatch.isEmpty)
  }

  test("should not be empty if there are git diffs") {
    val (input, tree, comments) = params(noEscapes)
    val diff = DiffDisable(List(NewFile(Paths.get("foo"))))
    val hatch = EscapeHatch(input, tree, comments, diff)

    assert(!hatch.isEmpty)
  }

  private def params(
      input: Input
  ): (Input, LazyValue[Tree], LazyValue[AssociatedComments]) = {
    val tree = input.parse[Source].get
    val comments = AssociatedComments.apply(tree)
    (input, LazyValue.now(tree), LazyValue.now(comments))
  }
}
