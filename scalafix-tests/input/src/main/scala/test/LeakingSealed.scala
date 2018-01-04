/*
rules = LeakingSealed
 */
package test

class LeakingSealed {
  sealed trait S
  class A extends S // assert: LeakingSealed
  class B extends S // assert: LeakingSealed
  trait C extends S // assert: LeakingSealed
  final class D extends S // ok
  sealed class E extends S // ok
  sealed trait F extends S // ok

  trait NotS // ok
  class G extends NotS // ok
  trait H extends NotS // ok
}
