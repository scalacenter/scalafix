---
layout: docs
title: MissingFinal
---

# MissingFinal

_Since 0.5.8_

This rule checks for or adds final modifier in the corresponding places.
It has two use cases:
- Check that descendants of a sealed types are final or sealed. 
- Add `final` modifier to case classes. 

The first case disallows the following situation:

`file 1:`
```scala
sealed trait t
trait a extends t // error: leaking sealed
class c extends t // error: leaking sealed
```

`file 2:`
```scala
trait b extends a // error: leaking sealed
class d extends c // error: leaking sealed                  
```