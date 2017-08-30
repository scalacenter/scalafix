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
  private var _attrs: Attributes = _
  implicit def sctx: SemanticCtx = _db
  def attrs: Attributes = _attrs
  def source: Source = attrs.input.parse[Source].get

  override def beforeAll(): Unit = {
    _db = SemanticCtx.load(Classpath(AbsolutePath(BuildInfo.sharedClasspath)))
    _attrs = _db.entries
      .find(_.input.label.contains(filename))
      .get
  }
}
