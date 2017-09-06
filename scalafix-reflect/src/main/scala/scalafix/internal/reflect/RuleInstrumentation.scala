package scalafix.internal.reflect

import scala.collection.immutable.Seq
import scala.meta._
import metaconfig.ConfError
import metaconfig.Configured

object RuleInstrumentation {

  def getRuleFqn(code: Input): Configured[Seq[String]] = {
    import scala.meta._
    object ExtendsRule {
      def unapply(templ: Template): Boolean = templ match {
        case Template(_, init"Rule" :: _, _, _) => true
        case Template(_, init"Rule" :: _, _, _) => true
        case Template(_, init"SemanticRule($_)" :: _, _, _) => true
        case Template(_, init"SemanticRule($_)" :: _, _, _) => true
        case _ => false
      }
    }
    object LambdaRule {
      def unapply(arg: Term): Boolean = arg match {
        case q"Rule.syntactic($_)" => true
        case q"Rule.semantic($_)" => true
        case q"Rule.syntactic($_)" => true
        case q"Rule.semantic($_)" => true
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
          case Defn.Object(_, name, ExtendsRule()) =>
            add(prefix :+ name.syntax)
          case Defn.Object(_, name, _) =>
            tree.children.foreach(s => loop(prefix :+ name.syntax, s))
          case Defn.Class(_, name, _, _, ExtendsRule()) =>
            add(prefix :+ name.syntax)
          case Defn.Val(_, Pat.Var(name) :: Nil, _, LambdaRule()) =>
            add(prefix :+ name.syntax)
          case _ =>
            tree.children.foreach(s => loop(prefix, s))
        }
        loop(Vector.empty, ast)
        val x = result.result()
        if (x.isEmpty) ConfError.msg(s"Found no rules in input $code").notOk
        else Configured.Ok(x)
    }
  }
}
