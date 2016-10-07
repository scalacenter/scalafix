package scalafix.util

import scala.annotation.tailrec
import scala.meta._
import scala.meta.internal.prettyprinters.Attributes
import scala.meta.semantic.Mirror

/** Super low-tech "solution" to make semantic info useful from a syntactic tree.
  *
  * The approach is roughly this:
  *
  * 1. Create "search index" (i.e., Map[String, String]) from a semantic Mirror.
  *    For example, the code
  *    {{{
  *       package bar
  *       class X {
  *         val x: foo.Y = new foo.Y
  *       }
  *    }}}
  *    creates index Map(bar.X.x -> foo.Y)
  * 2. Create string "query" from a syntactic tree node. Following the above
  *    example, to lookup the type of x the query is "bar.X.x".
  * 3. Lookup query in search index. Profit.
  */
class SemanticOracle(mirror: Mirror) {

  /** Returns the type annotation for the name, if any */
  def getType(name: Term.Name): Option[String] = {
    val query = getQuery(name)
    typeIndex.get(query)
  }

  private def getQuery(tree: Tree): String =
    "_root_." +
      TreeOps
        .getParents(tree)
        .map(getQueryPart)
        .filter(_.nonEmpty)
        .mkString(".")

  val escapedName = "`([\\w\\d_]+) `".r
  private def getQueryPart(tree: Tree): String = {
    val result = tree match {
      case Pkg(ref, _) => ref.syntax
      case Defn.Def(_, name, _, _, _, _) => name.syntax
      case Defn.Val(_, Seq(Pat.Var.Term(name)), _, _) => name.syntax
      case t: Defn.Class => t.name.syntax
      case t: Defn.Object => t.name.syntax + "$"
      case t: Defn.Trait => t.name.syntax
      case _ => ""
    }
    result match {
      case escapedName(name) => name
      case name => name.trim
    }
  }

  private def getQueries(tree: Tree): Seq[(String, String)] = {
    val builder = Seq.newBuilder[(String, String)]
    def loop(query: String)(curr: Tree): Unit = {
      val append = getQueryPart(curr)
      val nextQuery = if (append.nonEmpty) s"$query.$append" else query
      curr match {
        case Type.Name(name) =>
          builder += (query -> name)
        case els =>
      }
      curr.children.foreach(loop(nextQuery))
    }
    loop("_root_")(tree)
    builder.result()
  }

  private val typeIndex: Map[String, String] = {
    val builder = Map.newBuilder[String, String]
    mirror.domain.sources.foreach { source =>
      builder ++= getQueries(source)
    }
    builder.result()
  }

}
