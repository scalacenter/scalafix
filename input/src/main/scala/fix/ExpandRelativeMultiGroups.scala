/*
rules = [OrganizeImports]
OrganizeImports {
  groups = ["re:javax?\\.", "scala.", "*"]
  expandRelative = true
}
 */
package fix

import scala.util
import java.time.Clock
import util.control
import javax.management.JMX
import control.NonFatal

object ExpandRelativeMultiGroups
