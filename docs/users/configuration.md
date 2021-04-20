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

## Triggered configuration

Overlay rules and rule specific configuration for scalafix on compilation, which is enabled by `scalafixOnCompile := true`.
Every configuration can be overlaid by `triggered` prefix. With the following configuration, explicit scalafix invocation will run with `[ DisableSyntax, RemoveUnused ]` rules, while scalafix on compilation will run with `[ DisableSyntax ]` rule.

```scala
// .scalafix.conf
rules = [
  DisableSyntax,
  RemoveUnused
]
DisableSyntax.noFinalize = true

// `rules` on compilation
triggered.rules = [
  DisableSyntax
]
```