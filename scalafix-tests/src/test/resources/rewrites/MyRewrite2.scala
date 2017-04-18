import scalafix._
import scala.meta._

object Rewrites {
  val myRewrite = Rewrite[Any] { ctx =>
    ctx.tree.collect {
      case n: scala.meta.Name => ctx.rename(n, Term.Name(n.syntax + "1"))
    }.asPatch
  }

  val myRewrite2 = Rewrite[Mirror] { ctx =>
    ctx.addGlobalImport(importer"scala.collection.immutable.Seq")
  }
}
