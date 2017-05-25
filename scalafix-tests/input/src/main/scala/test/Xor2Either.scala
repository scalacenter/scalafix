/*
rewrites = Xor2Either
 */
package test

import scala.concurrent.Future
import cats.data.{Xor, XorT}
trait Xor2Either {
  type MyDisjunction = Xor[Int, String]
  val r: MyDisjunction = Xor.Right.apply("")
  val s: Xor[Int, String] = cats.data.Xor.Left(1 /* comment */ )
  val t: Xor[Int, String] = r.map(_ + "!")
  val nest: Seq[Xor[Int, cats.data.Xor[String, Int]]]
  val u: XorT[Future, Int, String] = ???
}
