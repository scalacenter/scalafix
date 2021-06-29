/*
rules = [OrganizeImports]
OrganizeImports.removeUnused = false
 */
package fix {
  package nested {
    import java.time.Clock
    import scala.collection.JavaConverters._
    import sun.misc.BASE64Encoder
    import scala.concurrent.ExecutionContext
    import javax.annotation.Generated

    object NestedPackageWithBraces
  }
}
