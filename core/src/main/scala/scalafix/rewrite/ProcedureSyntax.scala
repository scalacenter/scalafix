package scalafix.rewrite

import scala.meta.Defn
import scala.meta.Source
import scala.meta._

object ProcedureSyntax extends Rewrite {
  override def rewrite(code: String): String = {
    code.parse[Source] match {
      case Parsed.Success(ast) =>
        ast.transform {
          case t: Defn.Def if t.decltpe.exists(_.tokens.isEmpty) =>
            q"""..${t.mods} def ${t.name}[..${t.tparams}](...${t.paramss}): Unit = ${t.body}"""
        }.syntax
      case _ => code
    }
  }
}
