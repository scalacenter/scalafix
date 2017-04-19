package scalafix.reflect

import scala.collection.immutable.Seq
import scala.meta._

import metaconfig.ConfError
import metaconfig.Configured

object RewriteInstrumentation {

  def getRewriteFqn(code: Input): Configured[Seq[String]] = {
    import scala.meta._
    object ExtendsRewrite {
      def unapply(templ: Template): Boolean = templ match {
        case Template(_, ctor"Rewrite" :: _, _, _) => true
        case Template(_, ctor"SemanticRewrite($_)" :: _, _, _) => true
        case _ => false
      }
    }
    object LambdaRewrite {
      def unapply(arg: Term): Boolean = arg match {
        case q"Rewrite.syntactic($_)" => true
        case q"Rewrite.semantic($_)" => true
        case _ => false
      }
    }
    (dialects.Scala211, code).parse[Source] match {
      case parsers.Parsed.Error(pos, msg, details) =>
        ConfError.parseError(pos, msg).notOk
      case parsers.Parsed.Success(ast) =>
        val result = List.newBuilder[String]
        def add(name: Vector[String]): Unit = {
          result += name.mkString(".")
        }

        def loop(prefix: Vector[String], tree: Tree): Unit = tree match {
          case Pkg(ref, stats) =>
            stats.foreach(s => loop(prefix :+ ref.syntax, s))
          case Defn.Object(_, name, ExtendsRewrite()) =>
            add(prefix :+ name.syntax)
          case Defn.Object(_, name, _) =>
            tree.children.foreach(s => loop(prefix :+ name.syntax, s))
          case Defn.Class(_, name, _, _, ExtendsRewrite()) =>
            add(prefix :+ name.syntax)
          case Defn.Val(_, Pat.Var.Term(name) :: Nil, _, LambdaRewrite()) =>
            add(prefix :+ name.syntax)
          case _ =>
            tree.children.foreach(s => loop(prefix, s))
        }
        loop(Vector.empty, ast)
        val x = result.result()
        if (x.isEmpty) ConfError.msg(s"Found no rewrites in input $code").notOk
        else Configured.Ok(x)

    }
  }
}
