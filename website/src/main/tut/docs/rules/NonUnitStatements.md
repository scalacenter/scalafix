---
layout: docs
title: NonUnitStatements
---

# NonUnitStatements

_Since 0.5.8_

This rules forbids statements not returning `Unit`.
This ensures that they’re really intended to be statements. 

For example,

```scala
123
false
1 + 2
!false
List(1, 2, 3).map(_ * 2)
```
are all forbidden, but
```scala
println("Hello!")
System.gc()
```
are ok. 
