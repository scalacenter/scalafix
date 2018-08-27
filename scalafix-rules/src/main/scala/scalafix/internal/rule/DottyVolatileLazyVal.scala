package scalafix.internal.rule

import scala.meta._
import scalafix.v1._

class DottyVolatileLazyVal extends SyntacticRule("DottyVolatileLazyVal") {
  override def description: String =
    "Rewrite all lazy vals to Dotty's volatile ones for safe publishing (default semantics of pre-Dotty Scala)"
  private object NonVolatileLazyVal {
    def unapply(defn: Defn.Val): Option[Token] = {
      if (defn.parent.exists(!_.is[Template])) None
      else {
        defn.mods.collectFirst {
          case x if x.syntax == "@volatile" =>
            None
          case x if x.syntax == "lazy" =>
            Some(defn.mods.head.tokens.head)
        }
      }
    }.flatten
  }
  override def fix(implicit doc: Doc): Patch = {
    doc.tree.collect {
      case NonVolatileLazyVal(tok) =>
        Patch.addLeft(tok, s"@volatile ")
    }.asPatch
  }
}
