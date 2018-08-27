package scalafix.tests.core

import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite
import scala.meta._
import scala.meta.internal.io.PathIO
import scalafix.internal.v0.DocSemanticdbIndex
import scalafix.internal.v0.LegacyInMemorySemanticdbIndex
import scalafix.syntax._
import scalafix.testkit.DiffAssertions
import scalafix.v1.SemanticDoc

abstract class BaseSemanticSuite(filename: String)
    extends FunSuite
    with BeforeAndAfterAll
    with DiffAssertions {
  var _db: LegacyInMemorySemanticdbIndex = _
  var _input: Input = _
  implicit def doc: SemanticDoc =
    _db.index(_input.syntax) match {
      case sdoc: DocSemanticdbIndex => sdoc.doc
      case els => throw new IllegalArgumentException(els.toString)
    }
  implicit def index: LegacyInMemorySemanticdbIndex = _db
  def input: Input = _input
  def source: Source = {
    input.parse[Source].get
  }

  override def beforeAll(): Unit = {
    val dir = AbsolutePath(scalafix.tests.BuildInfo.sharedClasspath)
    _db = LegacyInMemorySemanticdbIndex.load(
      Classpaths.withDirectory(dir),
      PathIO.workingDirectory
    )
    _input = _db.inputs
      .collectFirst {
        case i if i.label.contains(filename) =>
          i
      }
      .getOrElse {
        throw new IllegalArgumentException(
          s"No $filename.semanticdb file found!"
        )
      }
  }
}
