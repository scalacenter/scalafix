---
id: configuration
title: Configuration
---

Scalafix reads configuration from a file `.scalafix.conf` in the root directory
of your project. Configuration is written using
[HOCON](https://github.com/lightbend/config) syntax.

## Configuring rules

Configure which rules to run with `rules = [ ... ]`, for example

```scala
// .scalafix.conf
rules = [
  DisableSyntax
]
```

## Rule-specific configuration

Some rules like `DisableSyntax` support custom configuration. Configuration for
rules should be at the top-level so `.scalafix.conf` will look something like
this

```scala
// .scalafix.conf
rules = [
  DisableSyntax
]
DisableSyntax.noFinalize = true
```
