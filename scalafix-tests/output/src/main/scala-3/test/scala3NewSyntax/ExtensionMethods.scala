
package scala3NewSyntax


import scala.util.{Try, Success}

object ExtensionMethods {
  extension [A] (ls: List[Try[A]])
    def collectSucceeded: List[A] =
      ls.collect { case Success(x) => x }
    def getIndexOfFirstFailure: Option[Int] =
      ls.zipWithIndex.find((t, _) => t.isFailure)
        .map(_._2)
}