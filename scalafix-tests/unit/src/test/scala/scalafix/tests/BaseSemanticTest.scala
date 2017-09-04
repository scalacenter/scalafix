package scalafix.tests

import scala.meta._
import scalafix.syntax._
import scalafix.util.SemanticCtx
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite

abstract class BaseSemanticTest(filename: String)
    extends FunSuite
    with BeforeAndAfterAll {
  private var _db: SemanticCtx = _
  private var _doc: Document = _
  implicit def sctx: SemanticCtx = _db
  def docs: Document = _doc
  def source: Source = docs.input.parse[Source].get

  override def beforeAll(): Unit = {
    _db = SemanticCtx.load(Classpath(AbsolutePath(BuildInfo.sharedClasspath)))
    _doc = _db.documents
      .find(_.input.label.contains(filename))
      .get
  }
}
