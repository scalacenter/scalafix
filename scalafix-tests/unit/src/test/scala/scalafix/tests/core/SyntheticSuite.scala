package scalafix.tests.core

import System.{lineSeparator => nl}

class SyntheticSuite extends BaseSemanticSuite("SyntheticTest") {

  test("text") {
    val synthetics = index.synthetics(input)
    val obtained = synthetics.map(_.text).mkString(nl)

    val expected =
      """|*(scala.collection.immutable.List.canBuildFrom[scala.Int])
         |*[scala.package.List]
         |*[scala.Int, scala.Predef.Set[scala.Int]]
         |*.scala.collection.immutable.List.apply[scala.Int]
         |*(scala.collection.immutable.Set.canBuildFrom[scala.Int])
         |*[scala.collection.immutable.List[scala.Int], scala.Int, scala.Predef.Set[scala.Int]]
         |*[scala.Int, scala.collection.immutable.List]
         |*.scala.collection.immutable.List.apply[scala.concurrent.Future[scala.Int]]
         |*(scala.concurrent.ExecutionContext.Implicits.global)
         |*.scala.concurrent.Future.apply[scala.Int]
         |*(scala.collection.immutable.List.canBuildFrom[scala.Int])
         |*[scala.collection.immutable.List[scala.concurrent.Future[scala.Int]], scala.Int, scala.collection.immutable.List[scala.Int]]""".stripMargin

    assertNoDiff(obtained, expected)
  }
}
