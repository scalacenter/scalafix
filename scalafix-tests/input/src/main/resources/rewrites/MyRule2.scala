import scalafix.v0._
import scala.meta._

object Rules {
  val myRule = Rule.syntactic { ctx =>
    ctx.tree.collect {
      case n: scala.meta.Name => ctx.rename(n, Term.Name(n.syntax + "1"))
    }.asPatch
  }

  val myRule2 = Rule.semantic { implicit index => ctx =>
    ctx.addGlobalImport(importer"scala.collection.immutable.Seq")
  }
}
