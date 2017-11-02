package scalafix.testkit.utest

import scala.meta.AbsolutePath
import scalafix.SemanticdbIndex
import scalafix.testkit.BaseSemanticRuleSuite

abstract class SemanticRuleSuite(
    val index: SemanticdbIndex,
    val expectedOutputSourceroot: Seq[AbsolutePath]
) extends ScalafixTest
    with BaseSemanticRuleSuite
