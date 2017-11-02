package scalafix.testkit.scalatest

import scalafix.SemanticdbIndex
import scalafix.internal.util.EagerInMemorySemanticdbIndex
import scalafix.testkit.BaseSemanticRuleSuite
import org.langmeta.io.AbsolutePath
import org.langmeta.io.Classpath
import org.langmeta.io.Sourcepath
import org.langmeta.semanticdb.Database

abstract class SemanticRuleSuite(
    val index: SemanticdbIndex,
    val expectedOutputSourceroot: Seq[AbsolutePath]
) extends BaseSemanticRuleSuite
    with ScalafixSuite {
  def this(
      index: SemanticdbIndex,
      inputSourceroot: AbsolutePath,
      expectedOutputSourceroot: Seq[AbsolutePath]
  ) = this(
    index,
    expectedOutputSourceroot
  )
  def this(
      database: Database,
      inputSourceroot: AbsolutePath,
      expectedOutputSourceroot: Seq[AbsolutePath]
  ) =
    this(
      EagerInMemorySemanticdbIndex(
        database,
        Sourcepath(inputSourceroot),
        Classpath(Nil)),
      inputSourceroot,
      expectedOutputSourceroot
    )
}
