---
layout: docs
title: LeakingSealed
---

# LeakingSealed

_Since 0.5.8_

This rule ensures that descendants of a sealed types are final or sealed. 

It disallows the following situation:

`file 1:`
```scala
sealed trait t
trait a extends t
class c extends t
```

`file 2:`
```scala
trait b extends a
class d extends c
```