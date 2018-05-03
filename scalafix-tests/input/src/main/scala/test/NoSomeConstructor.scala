/*
rule = NoSomeConstructor
 */
package test

import scala.{Some => MySome}

object NoSomeConstructor {
  MySome(42)
  Right(42)
}
