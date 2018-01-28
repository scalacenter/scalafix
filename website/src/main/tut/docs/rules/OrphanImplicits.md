---
layout: docs
title: DisableUnless
---

# OrphanImplicits

_Since 0.6.0_

This rule bans definitions of implicits F[G] unless they are on the companion of the F or the G.

# Example

```scala
  trait Foo
  trait Bar[T]

  object Foo {
    implicit val foo: Foo = ??? // ok
    implicit val listFoo: List[Foo] = ??? // ok
  }

  object Bar {
    implicit val foo: Foo = ??? // error
    implicit val barFoo: Bar[Foo] = ??? // ok
  }
```