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

When Scalafix is triggered automatically (`scalafixOnCompile := true`), the
values for keys prefixed with `triggered` takes precedence over the default
configuration - both for rules and rule-specific configuration. You can think
of `triggered` as a optional overlay, on top of the default configuration.
  
For example, with the following configuration, explicit scalafix invocation
will run `DisableSyntax` & `RemoveUnused`, while triggered invocations
will only run `DisableSyntax`. In both cases, the `noFinalize` configuration
is enabled.

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
