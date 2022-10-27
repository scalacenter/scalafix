package scalafix.internal.reflect

import scala.collection.immutable.Seq

import scala.meta._

import metaconfig.ConfError
import metaconfig.Configured
import scalafix.internal.config.MetaconfigOps._

object RuleInstrumentation {

  def getRuleFqn(code: Input): Configured[Seq[String]] = {
    object ExtendsRule {
      def unapply(templ: Template): Boolean = templ match {

        // v0
        case Template(_, Init(Type.Name("Rewrite"), _, Nil) :: _, _, _) => true
        case Template(
              _,
              Init(Type.Name("Rule"), _, List(List(_))) :: _,
              _,
              _
            ) =>
          true
        case Template(
              _,
              Init(Type.Name("SemanticRewrite"), _, List(List(_))) :: _,
              _,
              _
            ) =>
          true
        case Template(
              _,
              Init(Type.Name("SemanticRule"), _, List(List(_, _))) :: _,
              _,
              _
            ) =>
          true

        // v1
        case Template(
              _,
              Init(Type.Name("SemanticRule"), _, List(List(_))) :: _,
              _,
              _
            ) =>
          true
        case Template(
              _,
              Init(Type.Name("v1.SemanticRule"), _, List(List(_))) :: _,
              _,
              _
            ) =>
          true
        case Template(
              _,
              Init(Type.Name("SyntacticRule"), _, List(List(_))) :: _,
              _,
              _
            ) =>
          true
        case Template(
              _,
              Init(Type.Name("v1.SyntacticRule"), _, List(List(_))) :: _,
              _,
              _
            ) =>
          true

        case _ => false
      }
    }

    (dialects.Scala213, code).parse[Source] match {
      case parsers.Parsed.Error(pos, msg, details) =>
        ConfError.parseError(pos.toMetaconfig, msg).notOk
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
          case Defn.Class(_, name, _, _, ExtendsRule()) =>
            add(prefix :+ name.syntax)
          case _ =>
            tree.children.foreach(s => loop(prefix, s))
        }
        loop(Vector.empty, ast)
        val x = result.result()
        if (x.isEmpty) ConfError.message(s"Found no rules in input $code").notOk
        else Configured.Ok(x)
    }
  }
}
