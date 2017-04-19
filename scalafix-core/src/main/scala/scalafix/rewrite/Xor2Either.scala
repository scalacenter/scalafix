package scalafix
package rewrite
import scala.collection.immutable.Seq
import scala.meta.contrib._
import scala.meta.internal.ast.Helpers._
import scala.meta.{Symbol => _, _}
import scala.meta.semantic.v1._
import scalafix.syntax._

case class Xor2Either(implicit mirror: Mirror)
    extends SemanticRewrite(mirror) {
  override def rewrite(ctx: RewriteCtx): Patch = {
    val importImplicits = ctx.tree.collectFirst {
      case t: Term.Name
          if mirror
            .symbol(t)
            .toOption
            .exists(_.normalized == Symbol("_root_.cats.data.Xor.map.")) =>
        ctx.addGlobalImport(importer"cats.implicits._")
    }
    importImplicits.getOrElse(Patch.empty) ++
      Seq(
        ctx.replace(Symbol("_root_.cats.data.XorT."),
                    q"EitherT",
                    List(importer"cats.data.EitherT")),
        ctx.replace(Symbol("_root_.cats.data.Xor."), q"Either"),
        ctx.replace(Symbol("_root_.cats.data.Xor.Left."), q"Left"),
        ctx.replace(Symbol("_root_.cats.data.Xor.Right."), q"Right"),
        ctx.replace(Symbol("_root_.cats.data.XorFunctions.left."), q"Left"),
        ctx.replace(Symbol("_root_.cats.data.XorFunctions.right."), q"Right"),
        ctx.removeGlobalImport(importer"cats.data.Xor"),
        ctx.removeGlobalImport(importer"cats.data.XorT")
      )
  }
}
