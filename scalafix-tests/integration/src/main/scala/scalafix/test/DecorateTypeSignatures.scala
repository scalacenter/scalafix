package scalafix.test

import scala.meta._

import scalafix.v1._

class DecorateTypeSignatures extends SemanticRule("DecorateTypeSignatures") {

  override def fix(implicit doc: SemanticDocument): Patch =
    doc.tree.collect { case t: Defn.Type =>
      t.symbol.info
        .map(_.signature)
        .map {
          case sig: TypeSignature =>
            Patch.addLeft(t, "/*" + sig.upperBound.toString + "*/ ")
          case _ =>
            Patch.empty
        }
        .getOrElse(Patch.empty)
    }.asPatch

}
