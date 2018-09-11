---
id: configuration
title: Configuration
---

Scalafix reads configuration from a file using
[HOCON](https://github.com/typesafehub/config) syntax. The convention is to keep
a file `.scalafix.conf` into the root directory of your build listing the rules
you want to enforce in your project

## Configuring rules

Configure which rules to run with `rules = [ ... ]`, for example

```scala
rules = [
  // Built-in Scalafix rule
  DisableSyntax
  // custom rule on local disk
  "file:rules/MyRule.scala"
  // migration rewrite from library on GitHub
  "github:typelevel/cats/v1.0.0"
  // ...
]
```

Non-builtin rules are referenced using URI syntax. Scalafix supports loading
custom rules with the schemes `github:`, `replace:`, `class:`, `file:` and
`http:`.

### github:

Simple, one-time migration rewrites can be executed directly from GitHub.

```scala
rule = "github:typelevel/cats/v1.0.0"
// expands into "https://raw.githubusercontent.com/typelevel/cats/master/scalafix/rules/src/main/scala/fix/Cats_v1_0_0.scala"
```

### file:

If a rule is written in a single file on local disk, you can load it with the
`file:` protocol.

```scala
rule = "file:readme/MyRule.scala" // from local file on disk
```

## Rule-specific configuration

Some rules like `DisableSyntax` support custom configuration. Configuration for
rules should be at the top-level so `.scalafix.conf` will look something like
this

```conf
// .scalafix.conf
rules = [
  DisableSyntax
]
DisableSyntax.noFinalize = true
```
