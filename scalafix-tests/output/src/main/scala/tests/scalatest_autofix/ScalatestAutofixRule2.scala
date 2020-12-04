package tests.scalatest_autofix

import scala.collection.mutable
import org.scalatest_autofix.matchers
import org.scalatest_autofix.matchers.should.Matchers

object ScalatestAutofixRule2 {
  object WithRename {
    import org.scalatest_autofix.matchers.should.{Matchers => ScalaTestMatchers}

    class UsesRename extends ScalaTestMatchers
    class UsesOriginal extends matchers.should.Matchers {
      val x = mutable.ListBuffer.empty[Int]
    }
  }
  object WithoutRename {
    class UsesRename extends Matchers
    class UsesOriginal extends matchers.should.Matchers
  }
}
