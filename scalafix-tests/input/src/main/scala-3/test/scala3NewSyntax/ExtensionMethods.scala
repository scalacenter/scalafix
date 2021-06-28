/*
rules = [
  "Scala3NewSyntax"
]
*/
package scala3NewSyntax

import scala.util.{Try, Success}

object ExtensionMethods {
  implicit class ListTryExtension[A](private val ls: List[Try[A]]) extends AnyVal {
    def collectSucceeded: List[A] = ls.collect { case Success(a) => a }
    def getIndexOfFirstFailure: Option[Int] =
      ls.zipWithIndex.find { case (t, _) => t.isFailure }
        .map { case (_, index) => index }
  }
}