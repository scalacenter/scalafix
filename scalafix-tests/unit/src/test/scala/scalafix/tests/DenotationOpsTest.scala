package scalafix.tests

import scala.meta._
import scalafix.syntax._
import scalafix.internal.util.DenotationOps

class DenotationOpsTest extends BaseSemanticTest("DenotationOpsTest") {

  test("resultType") {
    val source = docs.input.parse[Source].get
    source.collect {
      case t @ Pat.Var(Name("x")) =>
        for {
          symbol <- t.symbol
          resultType <- symbol.resultType
        } yield assert(resultType.structure == Type.Name("Boolean").structure)

      case t @ Pat.Var(Name("y")) =>
        for {
          symbol <- t.symbol
          resultType <- symbol.resultType
        } yield assert(resultType.structure == Type.Apply(Type.Name("List"), List(Type.Name("String"))).structure)

      case t: Defn.Def if t.name.value == "m" =>
        for {
          symbol <- t.symbol
          resultType <- symbol.resultType
        } yield assert(resultType.structure == Type.Apply(Type.Name("List"), List(Type.Name("Int"))).structure)
    }
  }

}
