package test.removeUnused

package t1 {
  import scala.collection.immutable.{List => IList}

  object RenameUsed {
    IList.empty[String]
  }
}

package t2 {

  object RenameUnused
}

package t3 {
  import scala.util.{Success => _, _}

  object RenameUnusedWithUsedWildcard {
    Failure(new Exception())
  }
}

package t4 {

  object RenameUnusedWithUnusedWildcard
}

package t5 {

  object MultipleUnusedRenames
}

package t6 {
  import scala.concurrent.duration._

  object UnusedRenameFollowedByUsedWildcardInDifferentImports {
    Duration("10s")
  }
}

package t7 {
  import scala.io.StdIn._

  object UnusedRenameFollowingUsedWildcardInDifferentImports {
    lazy val l = readLine()
  }
}

package t8 {
  import scala.io._

  object UnusedRenameFollowedByUsedWildcardBetweenCommas {
    val c = Codec.UTF8
  }
}

package t9 {
  import java.util
  import util.concurrent
  import concurrent.atomic
  import atomic._

  object UnusedRenameWithUsedWildcardWhosePackagesAreImportedIndividually {
    new AtomicBoolean()
  }
}

package t10 {
  import java.{util => jutil}
  import jutil.{concurrent => jconcurrent}
  import jconcurrent.{locks => jlocks}
  import jlocks._

  object UnusedRenameWithUsedWildcardWhosePackagesAreImportedIndividuallyAndRenamed {
    new StampedLock()
  }
}
