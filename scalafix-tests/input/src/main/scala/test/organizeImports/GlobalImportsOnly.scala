/*
rules = [OrganizeImports]
OrganizeImports.removeUnused = false
 */
package test.organizeImports

import scala.collection.mutable
import scala.concurrent.duration.Duration
import java.time.Clock
import java.sql.DriverManager

object GlobalImportsOnly

// These imports should not be organized
import java.time.Duration, java.sql.Connection

object Foo
