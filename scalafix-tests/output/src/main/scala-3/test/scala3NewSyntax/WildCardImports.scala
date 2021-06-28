import scala.concurrent.duration.*
import scala.util.*

object WildCardImports:
  val oneMinute = 1.minute
  val maybe: Try[Unit] = Try(())
  val success: Try[Unit] = Success(())