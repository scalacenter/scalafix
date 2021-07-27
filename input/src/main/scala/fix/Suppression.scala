/*
rules = [OrganizeImports]
OrganizeImports.removeUnused = false
OrganizeImports.groups = ["re:javax?\\.", "*", "scala."]
 */
package fix

// scalafix:off
import java.time.Clock
import scala.collection.JavaConverters._
import sun.misc.Unsafe
import scala.concurrent.ExecutionContext
import javax.net.ssl
// scalafix:on

object Suppression
