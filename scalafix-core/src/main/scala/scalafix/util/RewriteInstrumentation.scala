package scalafix.util

import scala.meta._
import scala.collection.immutable.Seq
import scalafix.util.TreeExtractors._

object RewriteInstrumentation {

  // removes syntax that is not supported by toolbox
  private def simplify(tree: Tree): Tree = tree.transform {
    // packages are not supported by toolbox.
    case Source(Seq(Pkg(ref, stats))) => Source(q"import $ref._" +: stats)
    case Source(stats) =>
      Source(stats.flatMap {
        case Import(is) => is.map(i => Import(Seq(i)))
        case x => Seq(x)
      })
  }

  def instrument(code: String): String = {
    code.parse[Source] match {
      case parsers.Parsed.Success(ast) =>
        val rewriteName: Seq[String] = ast.collect {
          case Defn.Object(_,
                           name,
                           Template(_, ctor"Rewrite[$_]" :: _, _, _)) =>
            name.value
          case Defn.Val(_, Pat.Var.Term(name) :: Nil, _, q"Rewrite[$_]($_)")
                `:WithParent:` (_: Template)
                `:WithParent:` Defn.Object(_, parentName, _) =>
            s"$parentName.$name"
        }
        rewriteName match {
          case Nil =>
            sys.error(s"Found no rewrite in code! \n $code")
          case _ =>
            val folded = rewriteName.tail.foldLeft(rewriteName.head)(
              _ + ".andThen(" + _ + ")")
            val transformed = simplify(ast)
            s"""
               |${transformed.syntax}
               |
               |$folded
               |""".stripMargin
        }
      case _ => code
    }
  }
}
