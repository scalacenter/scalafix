/*
rules = RemoveUnused
 */
package test.removeUnused

package t1 {
  import scala.collection.immutable.{List => IList}

  object RenameUsed {
    IList.empty[String]
  }
}

package t2 {
  import scala.collection.immutable.{Set => ISet}

  object RenameUnused
}

package t3 {
  import scala.util.{Success => UnusedSuccess, _}

  object RenameUnusedWithUsedWildcard {
    Failure(new Exception())
  }
}

package t4 {
  import scala.concurrent.{Future => UnusedSuccess, _}

  object RenameUnusedWithUnusedWildcard
}

package t5 {
  import scala.collection.mutable.{Map => MutableMap, HashMap => MutableHashMap, HashSet => MutableHashSet}
  import scala.collection.mutable.{Map => MMap, HashMap => MHashMap, HashSet => MHashSet}

  object MultipleUnusedRenames
}

package t6 {
  import scala.concurrent.duration.{FiniteDuration => FDuration}
  import scala.concurrent.duration._

  object UnusedRenameFollowedByUsedWildcardInDifferentImports {
    Duration("10s")
  }
}

package t7 {
  import scala.io.StdIn._
  import scala.io.StdIn.{readByte => readL}

  object UnusedRenameFollowingUsedWildcardInDifferentImports {
    lazy val l = readLine()
  }
}

package t8 {
  import scala.io.{Source => S}, scala.io._

  object UnusedRenameFollowedByUsedWildcardBetweenCommas {
    val c = Codec.UTF8
  }
}

package t9 {
  import java.util.concurrent.atomic.{AtomicInteger => A}
  import java.util
  import util.concurrent
  import concurrent.atomic
  import atomic._

  object UnusedRenameWithUsedWildcardWhosePackagesAreImportedIndividually {
    new AtomicBoolean()
  }
}

package t10 {
  import java.util.concurrent.locks.{ReentrantLock => RL}
  import java.{util => jutil}
  import jutil.{concurrent => jconcurrent}
  import jconcurrent.{locks => jlocks}
  import jlocks._

  object UnusedRenameWithUsedWildcardWhosePackagesAreImportedIndividuallyAndRenamed {
    new StampedLock()
  }
}

