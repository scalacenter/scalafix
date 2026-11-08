package scalafix.tests.core

import scala.meta._
import scala.meta.contrib._
import scala.meta.contrib.equality.Structurally.StructuralEq

import scalafix.syntax._

// Scala 2 only: v0 denotations of global symbols come from the symbol table,
// which does not resolve symbols from classes compiled by Scala 3
class DenotationOpsSuite extends BaseSemanticSuite("DenotationOpsTest") {

  test("resultType") {
    def resultType(tree: Tree): Option[Type] =
      tree.symbol.flatMap(_.resultType)
    val listOfString =
      Type.Apply(Type.Name("List"), Type.ArgClause(List(Type.Name("String"))))

    val checked = source.collect {
      case t @ Pat.Var(Name("x")) =>
        assert(resultType(t).exists(_ isEqual Type.Name("Boolean")))
        "x"
      case t @ Pat.Var(Name("y")) =>
        assert(resultType(t).exists(_ isEqual listOfString))
        "y"
      case t: Defn.Def if t.name.value == "m" =>
        // the method signature "(x: Int, y: String)List[String]" does not
        // parse as a type
        assert(resultType(t.name).isEmpty)
        "m"
    }
    assert(checked.sorted == List("m", "x", "y"))
  }

}
