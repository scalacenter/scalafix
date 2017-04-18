import scala.meta._
import scalafix.rewrite._

package object scalafix {
  type SemanticRewriteCtx = RewriteCtx[Mirror]
  type SyntacticRewriteCtx = RewriteCtx[Any]
  implicit class XtensionSemanticRewriteCtx(val ctx: SemanticRewriteCtx)
      extends SemanticPatchOps
  implicit class XtensionRewriteCtx[T](val ctx: RewriteCtx[T])
      extends SyntacticPatchOps[T]
}
