package scalafix.rewrite

import scala.meta._
import scalafix.util.Patch

abstract class Rewrite {
  def rewrite(code: Tree, rewriteCtx: RewriteCtx): Seq[Patch]
}

object Rewrite {
  private def nameMap[T](t: sourcecode.Text[T]*): Map[String, T] = {
    t.map(x => x.source -> x.value).toMap
  }

  val name2rewrite: Map[String, Rewrite] = nameMap[Rewrite](
    ExplicitImplicit,
    ProcedureSyntax,
    VolatileLazyVal
  )

  val default: Seq[Rewrite] = name2rewrite.values.toSeq
}
