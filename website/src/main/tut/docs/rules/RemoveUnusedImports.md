---
layout: docs
title: RemoveUnusedImports
---
## RemoveUnusedImports

This rule acts upon "Unused import" warnings emitted by the Scala compiler. See [slick/slick/pulls#1736](https://github.com/slick/slick/pull/1736) for an example diff from running `sbt "scalafix RemoveUnusedImports"`.

To use this rule:

- enable `-Ywarn-unused-import`

- disable `-Xfatal-warnings`. Unfortunately, the Scala compiler does not support finer grained control over the severity level per message kind. See [scalameta/scalameta#924](https://github.com/scalameta/scalameta/issues/924) for a possible workaround in the near future.

```scala
// before
import scala.List
import scala.collection.{immutable, mutable}
object Foo { immutable.Seq.empty[Int] }

// after
import scala.collection.immutable
object Foo { immutable.Seq.empty[Int] }
```

__Note__. This rule does a best-effort at preserving original formatting. In some cases, the rewritten code may be formatted weirdly

```scala
// before
import scala.concurrent.{
  CancellationException,
  TimeoutException
}
// after
import scala.concurrent.

  TimeoutException
```

It's recommended to use a code formatter after running this rule.
