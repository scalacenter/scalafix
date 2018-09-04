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
  ExplicitResultTypes
  // custom rule on local disk
  "file:rules/MyRule.scala"
  // migration rewrite from library on GitHub
  "github:typelevel/cats/v1.0.0"
  // custom rule shared as a gist
  "https://git.io/vNXDG"
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

### replace:

To replace usage of one class/object/trait/def with another. Note, does not move
definitions like "Move" does in an IDE. This only moves use-sites.

```scala
rule = "replace:com.company.App/io.company.App"
// From sbt shell: > scalafix replace:from/to
```

To rename a method

```scala
rule = "replace:com.company.App.start/init"
```

Pros:

- Simple, no custom coding required

Cons:

- Limited functionality and the refactoring may not always produce the desired
  diff.

### class:

If a scalafix rule is already on the classpath, you can classload it with the
`scala:` protocol.

```scala
rule = "class:scalafix.internal.rule.ProcedureSyntax"
```

### file:

If a rule is written in a single file on local disk, you can load it with the
`file:` protocol.

```scala
rule = "file:readme/MyRule.scala" // from local file on disk
```

### http:

If a rule is written in a single source file on the internet, you can load it
with the `https:` or `http:` protocol

```scala
rule = "https://gist.githubusercontent.com/olafurpg/fc6f43a695ac996bd02000f45ed02e63/raw/f5fe47495c9b6e3ce0960b766ffa75be6d6768b2/DummyRule.scala"
```

## Rule-specific configuration

Some rules like `ExplicitResultTypes` support custom configuration.
Configuration for rules should be at the top-level so `.scalafix.conf` will look
something like this

```conf
// .scalafix.conf
rules = [
  ExplicitResultTypes
]
ExplicitResultTypes.skipSimpleDefinitions = true
```
