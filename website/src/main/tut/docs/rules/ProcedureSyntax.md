---
layout: docs
title: ProcedureSyntax
---

# ProcedureSyntax
"Procedure syntax" is not supported in Dotty.
Methods that use procedure syntax should use regular method syntax instead.
For example,

```scala
// before: procedure syntax
def main(args: Seq[String]) {
  println("Hello world!")
}
// after: regular syntax
def main(args: Seq[String]): Unit = {
  println("Hello world!")
}
```
