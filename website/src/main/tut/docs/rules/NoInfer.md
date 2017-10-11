---
layout: docs
title: NoInfer
---

# NoInfer

_Since 0.5.0_

This rule reports errors when the compiler inferred one of the following types:

- `Serializable`
- `Any`
- `AnyVal`
- `Product`

Example:

```scala
MyCode.scala:7: error: [NoInfer.any] Inferred Any
  List(1, "")
            ^
```

## Known limitations}

- Scalafix does not yet expose an way to disable rules across regions of code, track [#241](https://github.com/{{ site.githubOwner}}/{{ site.githubRepo}}/issues/241) for updates.
