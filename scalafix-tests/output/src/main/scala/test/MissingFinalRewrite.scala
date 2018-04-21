package test

object MissingFinalRewrite {
  final case class Bar(i: Int)
  final case class Ok(ok: String)
  @deprecated("oh no!", "") final case class BarD(i: Int)
  object FooBar {
    final case class Foo(s: String)
  }
  abstract case class Baz(baz: String)
  case class LeaveMeAlone(x: Int) // scalafix:ok MissingFinal

  // scenarios where case class may/may not hold reference to an outer class

  class Outer1 {
    case class Inner1()
  }

  class Outer2 {
    { final case class Inner2() }
  }

  trait Outer3 {
    case class Inner3()
  }

  trait Outer4 {
    def foo() = {
      final case class Inner4()
      "foo"
    }
  }

  object Outer5 {
    final case class Inner5()
  }

  class Outer6 {
    object Outer61 {
      object Outer611 {
        case class Inner6()
      }
    }
  }

  trait Outer7 {
    object Outer71 {
      object Outer711 {
        case class Inner7()
      }
    }
  }

  object Outer8 {
    object Outer81 {
      object Outer811 {
        final case class Inner8()
      }
    }
  }

  object Outer9 {
    class Outer91 {
      object Outer911 {
        case class Inner9()
      }
    }
  }
}
