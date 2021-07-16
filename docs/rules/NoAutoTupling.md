---
layout: docs
id: NoAutoTupling
title: NoAutoTupling
---

Adds explicit tuples around argument lists where auto-tupling is occurring.

To use this rule:

- enable `-Ywarn-adapted-args` for Scala 2.11 and 2.12 (note, `-Yno-adapted-args` will fail compilation,
  which prevents scalafix from running). For Scala 2.13, use instead `-Xlint:adapted-args`.
- enable also `-deprecation` to get warnings on insertions of `Unit`.

```scala
// before
def someMethod(t: (Int, String)) = ...
someMethod(1, "something")
val c: Option[Unit] = Some()
// after
def someMethod(t: (Int, String)) = ...
someMethod((1, "something"))
val c: Option[Unit] = Some(())
```

Auto-tupling is a feature that can lead to unexpected results, making code to
compile when one would expect a compiler error instead. Adding explicit tuples
makes it more obvious.

> Some auto-tupling cases are left unfixed, namely the ones involving
> constructor application using `new`

```scala
case class Foo(x: (String, Boolean))
new Foo("string", true) // won't be fixed
Foo("string", true)     // will be fixed
```

This is a known limitation.
