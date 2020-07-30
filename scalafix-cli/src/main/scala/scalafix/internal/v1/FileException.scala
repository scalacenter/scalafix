package scalafix.internal.v1

import scala.util.control.NoStackTrace

import scala.meta.io.AbsolutePath

case class FileException(file: AbsolutePath, cause: Throwable)
    extends Exception(s"unexpected error processing file $file", cause)
    with NoStackTrace
