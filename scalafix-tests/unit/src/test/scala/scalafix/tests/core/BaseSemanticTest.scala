package scalafix.tests.core

import scalafix.v0._
import scala.meta._
import scalafix.syntax._
import scalafix.tests.BuildInfo
import scalafix.util.SemanticdbIndex
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite
import scalafix.internal.v0.LegacyInMemorySemanticdbIndex
import scalafix.testkit.DiffAssertions

abstract class BaseSemanticTest(filename: String)
    extends FunSuite
    with BeforeAndAfterAll
    with DiffAssertions {
  private var _db: SemanticdbIndex = _
  private var _input: Input = _
  implicit def index: SemanticdbIndex = _db
  def input: Input = _input
  def source: Source = input.parse[Source].get

  override def beforeAll(): Unit = {
    _db = LegacyInMemorySemanticdbIndex.load(
      Classpath(AbsolutePath(BuildInfo.sharedClasspath)))
    _input = _db.inputs
      .find(_.label.contains(filename))
      .getOrElse {
        throw new IllegalArgumentException(
          s"No $filename.semanticdb file found! Files are ${_db.documents.map(_.input.label)}")
      }
  }
}
