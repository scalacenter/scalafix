package scalafix.internal.v1

import scala.meta.io.AbsolutePath
import scala.util.control.NoStackTrace

case class FileException(file: AbsolutePath, cause: Throwable)
    extends Exception(s"unexpected error processing file $file", cause)
    with NoStackTrace
