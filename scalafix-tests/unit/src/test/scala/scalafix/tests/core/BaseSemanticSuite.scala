package scalafix.tests.core

import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite
import scala.meta._
import scala.meta.internal.io.PathIO
import scalafix.internal.v0.LegacyInMemorySemanticdbIndex
import scalafix.syntax._
import scalafix.testkit.DiffAssertions
import scalafix.util.SemanticdbIndex

abstract class BaseSemanticSuite(filename: String)
    extends FunSuite
    with BeforeAndAfterAll
    with DiffAssertions {
  private var _db: SemanticdbIndex = _
  private var _input: Input = _
  implicit def index: SemanticdbIndex = _db
  def input: Input = _input
  def source: Source = {
    input.parse[Source].get
  }

  override def beforeAll(): Unit = {
    _db = LegacyInMemorySemanticdbIndex.load(
      Classpath(AbsolutePath(scalafix.tests.BuildInfo.sharedClasspath)),
      PathIO.workingDirectory
    )
    _input = _db.inputs
      .collectFirst {
        case i if i.label.contains(filename) =>
          i
      }
      .getOrElse {
        throw new IllegalArgumentException(
          s"No $filename.semanticdb file found! Files are ${_db.documents.map(_.input.label)}"
        )
      }
  }
}
