package test

class NoUnitInsertion {

  val x: Option[Unit] = Option(())

  def a(u: Unit): Unit = u
  a(())

  def b(x: Int)(u: Unit): Unit = (x, u)
  b(2)(())

  val c: Unit => Unit =
    u => u
  c(())

  case class Foo(u: Unit)
  Foo(())
  Foo.apply(())

  case class Bar(i: Int)(u: Unit)
  Bar.apply(2)(())

}
