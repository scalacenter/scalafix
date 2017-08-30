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
  private var _docs: Document = _
  implicit def sctx: SemanticCtx = _db
  def docs: Document = _docs
  def source: Source = docs.input.parse[Source].get

  override def beforeAll(): Unit = {
    _db = SemanticCtx.load(Classpath(AbsolutePath(BuildInfo.sharedClasspath)))
    _docs = _db.documents
      .find(_.input.label.contains(filename))
      .get
  }
}
