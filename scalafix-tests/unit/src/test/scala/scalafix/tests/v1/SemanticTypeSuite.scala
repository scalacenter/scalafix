package scalafix.tests.v1

import munit.FunSuite
import scalafix.v1._

class SemanticTypeSuite extends FunSuite {

  private val intRef = TypeRef(NoType, Symbol("scala/Int#"), Nil)
  private val listRef = TypeRef(NoType, Symbol("scala/package.List#"), Nil)

  test("applied annotated type") {
    val annotation =
      Annotation(TypeRef(NoType, Symbol("scala/deprecated#"), Nil))
    val obtained =
      AppliedType(AnnotatedType(List(annotation), listRef), List(intRef))
    assertEquals(obtained.toString, "(List @deprecated)[Int]")
  }

  test("applied type reference with arguments") {
    val function1 = TypeRef(NoType, Symbol("scala/Function1#"), List(intRef))
    val obtained = AppliedType(function1, List(intRef))
    assertEquals(obtained.toString, "Function1[Int][Int]")
  }

}
