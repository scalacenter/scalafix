import scalafix._
import scala.meta._

object Rewrites {
  val myRewrite = Rule.syntactic { ctx =>
    ctx.tree.collect {
      case n: scala.meta.Name => ctx.rename(n, Term.Name(n.syntax + "1"))
    }.asPatch
  }

  val myRewrite2 = Rule.semantic { implicit sctx => ctx =>
    ctx.addGlobalImport(importer"scala.collection.immutable.Seq")
  }
}
