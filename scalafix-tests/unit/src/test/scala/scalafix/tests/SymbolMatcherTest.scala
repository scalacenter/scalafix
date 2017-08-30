package scalafix.tests

import scalafix.util.SemanticCtx
import scalafix.util.SymbolMatcher
import scala.meta._
import scalafix.syntax._

class SymbolMatcherTest extends org.scalatest.FunSuite {

  test("matches/unapply") {
    implicit val db: SemanticCtx =
      SemanticCtx.load(Classpath(AbsolutePath(BuildInfo.semanticClasspath)))
    val explicitReturn =
      SymbolMatcher(Symbol("_root_.test.ExplicitReturnTypes."))
    val attrs: Attributes = db.entries
      .find(_.input.label.contains("ExplicitReturnTypes.scala"))
      .get
    val doc: Source = attrs.input.parse[Source].get
    val assertions = doc.collect {
      case explicitReturn(t @ Name(_)) if t.matches(explicitReturn) =>
        assert(t.is[Term.Name])
        assert(t.parent.get.is[Defn.Object])
    }
    assert(assertions.nonEmpty)
  }

}
