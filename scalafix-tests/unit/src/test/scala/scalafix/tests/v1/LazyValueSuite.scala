package scalafix.tests.v1
import org.scalatest.FunSuite
import scalafix.internal.v1.LazyValue

class LazyValueSuite extends FunSuite {
  test("now") {
    var i = 0
    val now = LazyValue.now {
      i += 1
      "hello"
    }
    assert(i == 1) // thunk is evaluated
    now.value
    assert(i == 1)
    now.value
    assert(i == 1) // thunk is not re-evaluated
    assert(now.value == "hello")
  }

  test("later") {
    var i = 0
    val later = LazyValue.later { () =>
      i += 1
      "hello"
    }
    assert(i == 0) // thunk is not evaluated
    later.value
    assert(i == 1)
    later.value
    assert(i == 1) // thunk is not re-evaluated
    assert(later.value == "hello")
  }

  test("map") {
    var i = 0
    val a = LazyValue.later { () =>
      i += 1
      "Hello "
    }

    var j = 0
    val b = LazyValue.later { () =>
      j += 1
      a.value + "world"
    }

    assert(i == 0)
    assert(j == 0)

    assert(a.value == "Hello ")
    assert(i == 1)
    assert(j == 0)

    assert(b.value == "Hello world")
    assert(i == 1)
    assert(j == 1)

    assert(b.value == "Hello world")
    assert(i == 1)
    assert(j == 1)

  }

  test("exception") {
    var i = 0
    val later = LazyValue.later { () =>
      i += 1
      sys.error("boom")
    }
    assert(i == 0)

    val boom1 = intercept[RuntimeException](later.value)
    assert(boom1.getMessage == "boom")
    assert(i == 1)

    val boom2 = intercept[RuntimeException](later.value)
    assert(boom1 eq boom2, "new exception thrown")
    assert(i == 1) // exception throwing thunk is evaluated only once

  }
}
