package test

import scala.collection.immutable.Seq
object FlipEither {
  val x: Either[String, Int] = if (true) Right(1) else Left("msg")
}
