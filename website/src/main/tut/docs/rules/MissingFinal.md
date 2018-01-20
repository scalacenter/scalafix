---
layout: docs
title: MissingFinal
---

# MissingFinal

_Since 0.5.8_

This rule checks for or adds final modifier in corresponding places.
It has two use cases:
- Check that descendants of a sealed types are final or sealed. And add `final` modifier to classes. 

It disallows the following situation:

`file 1:`
```scala
sealed trait t
trait a extends t // error: leaking sealed
class c extends t // error: leaking sealed
final class e extends t // ok
```

`file 2:`
```scala
trait b extends a // error: leaking sealed
class d extends c // error: leaking sealed                  
```

This rule reports errors when a "disallowed" syntax is used. This is a syntactic rule, which means it does not require compilation to run unlike the `Disable` rule.

Example:

```scala
MyCode.scala:7: error: [DisableSyntax.xml] xml is disabled.
  <a>xml</a>
  ^
```

```scala
MyCode.scala:10: error: [DisableSyntax.keywords.return] return is disabled.
  return
  ^
```

## Configuration

By default, this rule does not disable syntax.

It contains the following elements:

* keywords such as: `null, throw, var, return`
* semicolons (`;`)
* tabs
* xml literals
* covariant and contravariant type parameters (_Since 0.5.8_)
* default args in methods (_Since 0.5.8_)
* val definitions in traits and abstract classes (_Since 0.5.8_)
* implicit objects (_Since 0.5.8_)
* implicit conversions (implicit def with non-implicit parameters) (_Since 0.5.8_)

There're also rules that rewrite syntax:

* transforming `final val` to `val` (It breaks incremental compilation, see more <https://github.com/sbt/zinc/issues/227>) (_Since 0.5.8_)
 
To disallow a syntax:

```
DisableSyntax.keywords = [
  var
  null
  return
  throw
]
DisableSyntax.noTabs = true
DisableSyntax.noSemicolons = true
DisableSyntax.noXml = true
DisableSyntax.noCovariantTypes = true
DisableSyntax.noContravariantTypes = true
DisableSyntax.noDefaultArgs = true
DisableSyntax.noValInAbstract = true
DisableSyntax.noImplicitObject = true
DisableSyntax.noImplicitConversion = true
DisableSyntax.noFinalVal = true
```