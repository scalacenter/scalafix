/*
rules = "ExplicitResultTypes"
*/
package test.explicitResultTypes

// https://github.com/scalacenter/scalafix/issues/1219
object ExplicitResultTypesBackQuote {
  import ImportObject._

  case class `Foo-Bar`[A](i: A)
  trait `Foo-Trait` {}
  class `Foo-Class`
  final case class `Bar[,?! $42Baz`[F[_], G[_]](fi: F[Int], gs: G[String])

  object Nested {
    object Inner {
      case class `Qux Qux`(i: Int)
      final case class `Qux.Qux`[F[_]](fi: F[Int])
      trait `Nested-Trait`[F[_]] {}
    }
  }

  object ImportObject {
    case class `Quux !`(i: Int)
  }

  val foobar = `Foo-Bar`(42)
  val fooTrait = null.asInstanceOf[`Foo-Trait`]
  val fooClass: `Foo-Class` = null.asInstanceOf[`Foo-Class`]
  val barbaz = `Bar[,?! $42Baz`[List, Option](List(42), Some(""))
  val qux = Nested.Inner.`Qux Qux`(42)
  val quxqux = Nested.Inner.`Qux.Qux`[List](List(42))
  val quux = `Quux !`(42)
  val nestedTrait = null.asInstanceOf[Nested.Inner.`Nested-Trait`[`Foo-Bar`]]
  def byname[A] = null.asInstanceOf[(=> `Foo-Bar`[A]) => `Quux !`]
}
