package scalafix.internal.config

import metaconfig.ConfDecoder
import org.langmeta._

trait Config[A] {
  val symbols: List[Symbol]
}

object Config {
  
  def apply[A](xs: List[Symbol]): Config[A] =
    new Config[A] { val symbols = xs }
  
  def empty[A]: Config[A] =
    apply[A](Nil)

  def decoder[A]: ConfDecoder[Config[A]] =
    ConfDecoder.instanceF[Config[A]] {
      _.getOrElse[List[Symbol.Global]]("symbols")(Nil) map apply[A]
    }
}
