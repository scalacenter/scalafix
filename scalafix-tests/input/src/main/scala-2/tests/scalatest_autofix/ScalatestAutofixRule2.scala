/*
rule = ScalatestAutofixRule
*/
package tests.scalatest_autofix

import scala.collection.mutable

object ScalatestAutofixRule2 {
  object WithRename {
    import org.scalatest_autofix.{Matchers => ScalaTestMatchers}

    class UsesRename extends ScalaTestMatchers
    class UsesOriginal extends org.scalatest_autofix.Matchers {
      val x = mutable.ListBuffer.empty[Int]
    }
  }
  object WithoutRename {
    import org.scalatest_autofix.Matchers
    class UsesRename extends Matchers
    class UsesOriginal extends org.scalatest_autofix.Matchers
  }
}
