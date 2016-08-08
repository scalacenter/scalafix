package scalafix.rewrite

import scala.meta._
import scalafix.FixResult

object ProcedureSyntax extends Rewrite {
  override def rewrite(code: String): FixResult = {
    withParsed(code) { ast =>
      FixResult.Success {
        ast.transform {
          case t: Defn.Def if t.decltpe.exists(_.tokens.isEmpty) =>
            q"..${t.mods} def ${t.name}[..${t.tparams}](...${t.paramss}): Unit = ${t.body}"
        }.syntax
      }
    }
  }
}
