/*
rules = OrganizeImports

OrganizeImports.groups = [
  "re:javax?\\."
  "scala."
]
 */

package fix

import java.time.Clock
import scala.collection.JavaConverters._, sun.misc.BASE64Encoder
import java.time.{Duration, LocalDate}
import scala.concurrent.ExecutionContext
import scala.util
import util.control
import control.NonFatal
import javax.annotation.Generated
import scala.Predef.{println => printLine, ??? => _}

object OrganizeImports
