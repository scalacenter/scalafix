---
layout: docs
title: scalafix-core
---

## scalafix-core
Scalafix can be used as a library to run custom rules.

```scala
// ===> build.sbt
libraryDependencies += "ch.epfl.scala" %% "scalafix-core" % "@V.stableVersion"
// (optional) Scala.js is also supported
libraryDependencies += "ch.epfl.scala" %%% "scalafix-core" % "@V.stableVersion"
```

Example usage of the syntactic API.

```scala
{% include MyRule.scala %}
```

```scala
println(MyRule.Uppercase("object Hello { println('world) }"))
// object HELLO { PRINTLN('world) }
```

The semantic API requires a more complicated setup. Please
use {% doc_ref Giter8 template %}.
