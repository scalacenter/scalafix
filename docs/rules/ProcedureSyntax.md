---
layout: docs
id: ProcedureSyntax
title: ProcedureSyntax
---

"Procedure syntax" is a deprecated Scala feature that allows methods to leave
out the result type and assignment character `=`. For example,

```scala
def debug { println("debug") }
```

This rule replaces procedure syntax with an explicit `: Unit` result type for
both method implementations and abstract declaration.

```scala
// before: procedure syntax
def main(args: Seq[String]) { println("Hello world!") }
trait A { def doSomething }

// after: regular syntax
def main(args: Seq[String]): Unit = { println("Hello world!") }
trait A { def doSomething: Unit }
```

This syntax no longer exists in Scala 3 and therefore this rule only makes sense in Scala 2.
You need to remove `ProcedureSyntax` from `.scalafix.conf` for Scala 3 projects.
