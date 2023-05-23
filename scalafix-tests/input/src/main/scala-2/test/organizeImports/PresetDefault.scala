/*
rules = [OrganizeImports]
OrganizeImports {
  preset = DEFAULT
  groupedImports = Merge
  removeUnused = false
}
 */
package test.organizeImports

import scala.collection.mutable
import scala.util.Try
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Collections.binarySearch
import java.util.Collections.emptyList
import javax.management.MXBean
import test.organizeImports.PresetDefault.a

object PresetDefault {
  val a: Any = ???
}
