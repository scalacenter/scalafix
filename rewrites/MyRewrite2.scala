import scalafix._
import scalafix.rewrite._
import scalafix.util._
import scalafix.util.TreePatch._
import scala.collection.immutable.Seq
import scala.meta._
import scalafix.config.ScalafixConfig

object Rewrites {
  val myRewrite = Rewrite[Any] { ctx =>
    ctx.tree.collect {
      case n: scala.meta.Name => Rename(n, Term.Name(n.syntax + "1"))
    }
  }

  val myRewrite2 = Rewrite[Any] { ctx =>
    Seq(
      AddGlobalImport(importer"scala.collection.immutable.Seq")
    )
  }
}
