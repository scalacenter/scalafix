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
      j  += 1
      a.value + "world"
    }

    assert(i == 0)
    assert(j == 0)

    a.value
    assert(i == 1)
    assert(j == 0)

    b.value
    assert(i == 1)
    assert(j == 1)

    b.value
    assert(i == 1)
    assert(j == 1)

  }
}
