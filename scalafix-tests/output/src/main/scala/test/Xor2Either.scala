package test

import cats.implicits._
import cats.data.EitherT
import scala.concurrent.Future

trait Xor2Either {
  type MyDisjunction = Either[Int, String]
  val r: MyDisjunction = Right("")
  val s: Either[Int, String] = Left(1 /* comment */ )
  val t: Either[Int, String] = r.map(_ + "!")
  val nest: Seq[Either[Int, Either[String, Int]]]
  val u: EitherT[Future, Int, String] = ???
}
