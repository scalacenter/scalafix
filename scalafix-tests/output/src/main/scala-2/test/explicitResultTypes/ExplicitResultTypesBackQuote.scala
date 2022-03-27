package test.explicitResultTypes

// https://github.com/scalacenter/scalafix/issues/1219
object ExplicitResultTypesBackQuote {
  case class `Foo-Bar`(i: Int)
  final case class `Bar[,?! $42Baz`[F[_], G[_]](fi: F[Int], gs: G[String])

  val foobar: `Foo-Bar` = `Foo-Bar`(42)
  val barbaz: `Bar[,?! $42Baz`[List, Option] = `Bar[,?! $42Baz`[List, Option](List(42), Some(""))
}
