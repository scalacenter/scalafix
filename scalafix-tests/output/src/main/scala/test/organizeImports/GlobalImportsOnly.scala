package test.organizeImports

import java.sql.DriverManager
import java.time.Clock
import scala.collection.mutable
import scala.concurrent.duration.Duration

object GlobalImportsOnly

// These imports should not be organized
import java.time.Duration, java.sql.Connection

object Foo
