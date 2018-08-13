package scalafix.tests.core

import System.{lineSeparator => nl}

class SyntheticSuite extends BaseSemanticSuite("SyntheticTest") {

  test("text") {
    val synthetics = index.synthetics(input)
    val obtained = synthetics.mkString(nl)
    val expected =
      """|[268..274): *.scala.concurrent.Future.apply[scala.Int]
         |  [0..1): * => _star_.
         |  [2..31): scala.concurrent.Future.apply => scala/concurrent/Future.apply().
         |  [32..41): scala.Int => scala/Int#
         |[280..288): *[scala.collection.immutable.List[scala.concurrent.Future[scala.Int]], scala.Int, scala.collection.immutable.List[scala.Int]](scala.collection.immutable.List.canBuildFrom[scala.Int])
         |  [0..1): * => _star_.
         |  [2..33): scala.collection.immutable.List => scala/collection/immutable/List#
         |  [34..57): scala.concurrent.Future => scala/concurrent/Future#
         |  [58..67): scala.Int => scala/Int#
         |  [71..80): scala.Int => scala/Int#
         |  [82..113): scala.collection.immutable.List => scala/collection/immutable/List#
         |  [114..123): scala.Int => scala/Int#
         |  [126..170): scala.collection.immutable.List.canBuildFrom => scala/collection/immutable/List.canBuildFrom().
         |  [171..180): scala.Int => scala/Int#
         |[247..262): *[scala.Int, scala.collection.immutable.List]
         |  [0..1): * => _star_.
         |  [2..11): scala.Int => scala/Int#
         |  [13..44): scala.collection.immutable.List => scala/collection/immutable/List#
         |[268..277): *(scala.concurrent.ExecutionContext.Implicits.global)
         |  [0..1): * => _star_.
         |  [2..52): scala.concurrent.ExecutionContext.Implicits.global => scala/concurrent/ExecutionContext.Implicits.global.
         |[224..232): *[scala.collection.immutable.List[scala.Int], scala.Int, scala.Predef.Set[scala.Int]](scala.collection.immutable.Set.canBuildFrom[scala.Int])
         |  [0..1): * => _star_.
         |  [2..33): scala.collection.immutable.List => scala/collection/immutable/List#
         |  [34..43): scala.Int => scala/Int#
         |  [46..55): scala.Int => scala/Int#
         |  [57..73): scala.Predef.Set => scala/Predef.Set#
         |  [74..83): scala.Int => scala/Int#
         |  [86..129): scala.collection.immutable.Set.canBuildFrom => scala/collection/immutable/Set.canBuildFrom().
         |  [130..139): scala.Int => scala/Int#
         |[209..215): *[scala.Int, scala.Predef.Set[scala.Int]]
         |  [0..1): * => _star_.
         |  [2..11): scala.Int => scala/Int#
         |  [13..29): scala.Predef.Set => scala/Predef.Set#
         |  [30..39): scala.Int => scala/Int#
         |[263..267): *.scala.collection.immutable.List.apply[scala.concurrent.Future[scala.Int]]
         |  [0..1): * => _star_.
         |  [2..39): scala.collection.immutable.List.apply => scala/collection/immutable/List.apply().
         |  [40..63): scala.concurrent.Future => scala/concurrent/Future#
         |  [64..73): scala.Int => scala/Int#
         |[200..205): *[scala.package.List](scala.collection.immutable.List.canBuildFrom[scala.Int])
         |  [0..1): * => _star_.
         |  [2..20): scala.package.List => scala/package.List#
         |  [22..66): scala.collection.immutable.List.canBuildFrom => scala/collection/immutable/List.canBuildFrom().
         |  [67..76): scala.Int => scala/Int#""".stripMargin
    assertNoDiff(obtained, expected)
  }
}
