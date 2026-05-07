package scalafix.internal.reflect

import scala.meta._

import metaconfig.ConfError
import metaconfig.Configured
import scalafix.internal.config.MetaconfigOps._

object RuleInstrumentation {

  def getRuleFqn(code: Input): Configured[Seq[String]] = {
    object ExtendsRule {
      def unapply(templ: Template): Boolean = {
        val init = templ.inits.head
        (init.tpe, init.argClauses) match {
          case (Type.Name("Rewrite"), Seq()) => true /* v0 */
          case (Type.Name("Rule"), Seq(ac)) =>
            ac.values.lengthCompare(1) == 0 /* v0 */
          case (Type.Name("SemanticRewrite"), Seq(ac)) =>
            ac.values.lengthCompare(1) == 0 /* v0 */
          case (Type.Name("SemanticRule"), Seq(ac)) =>
            val len = ac.values.length
            len == 1 /* v1 */ || len == 2 /* v0 */
          case (Type.Name("v1.SemanticRule"), Seq(ac)) =>
            ac.values.lengthCompare(1) == 0 /* v1 */
          case (Type.Name("SyntacticRule"), Seq(ac)) =>
            ac.values.lengthCompare(1) == 0 /* v1 */
          case (Type.Name("v1.SyntacticRule"), Seq(ac)) =>
            ac.values.lengthCompare(1) == 0 /* v1 */
          case _ => false
        }
      }
    }

    (dialects.Scala213, code).parse[Source] match {
      case parsers.Parsed.Error(pos, msg, _) =>
        ConfError.parseError(pos.toMetaconfig, msg).notOk
      case parsers.Parsed.Success(ast) =>
        val result = List.newBuilder[String]
        def add(name: Vector[String]): Unit = {
          result += name.mkString(".")
        }

        def loop(prefix: Vector[String], tree: Tree): Unit = tree match {
          case t: Pkg =>
            t.body.stats.foreach(s => loop(prefix :+ t.ref.syntax, s))
          case Defn.Object(_, name, ExtendsRule()) =>
            add(prefix :+ name.syntax)
          case Defn.Class.Initial(_, name, _, _, ExtendsRule()) =>
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
