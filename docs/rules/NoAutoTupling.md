---
layout: docs
id: NoAutoTupling
title: NoAutoTupling
---

Adds explicit tuples around argument lists where auto-tupling is occurring.

To use this rule:

- enable `-Ywarn-adapted-args` (note, `-Yno-adapted-args` will fail compilation,
  which prevents scalafix from running)

- disable `-Xfatal-warnings`. Unfortunately, the Scala compiler does not support
  finer grained control over the severity level per message kind. See
  [scalameta/scalameta#924](https://github.com/scalameta/scalameta/issues/924)
  for a possible workaround in the near future.

```scala
// before
def someMethod(t: (Int, String)) = ...
someMethod(1, "something")
// after
def someMethod(t: (Int, String)) = ...
someMethod((1, "something"))
```

Auto-tupling is a feature that can lead to unexpected results, making code to
compile when one would expect a compiler error instead. Adding explicit tuples
makes it more obvious.

**Note**. Some auto-tupling cases are left unfixed, namely the ones involving
constructor application using `new`

```scala
case class Foo(x: (String, Boolean))
new Foo("string", true) // won't be fixed
Foo("string", true)     // will be fixed
```

This is a known limitation.
