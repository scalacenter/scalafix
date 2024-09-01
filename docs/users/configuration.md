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

## Overriding the linting behavior

By default, only lints raised with `LintSeverity.Error` will result in a
non-zero exit code. To force Scalafix to return a non-zero exit code for other
diagnostics, it is possible to promote non-error diagnostics to the error level
without touching the rules(s) source code, thanks to `lint.error`.

This key accepts as value either
* a string, interpreted as a regular expression to match diagnostics using the
  `<rule_name>[.<lint_id>]` pattern,
* a list of strings, treated as several regular expressions like above,
* an object, with `include` and `exclude` attributes, each treated as regular
  expressions like above.

As an example, to force all linter reports to trigger an error, except for unused
suppressions, any of the 3 configuration files below can be used, as they are
effectively equivalent.

```scala
// .scalafix.conf
lint.error = "^((?!UnusedScalafixSuppression).)*$"
```

```scala
// .scalafix.conf
lint.error = [
  "^((?!UnusedScalafixSuppression).)*$"
]
```

```scala
// .scalafix.conf
lint.error = {
  includes = ".*"
  excludes = "UnusedScalafixSuppression"
}
```

Similarly, diagnostics can be controlled through `lint.ignore`, `lint.info`,
and `lint.warning`, which respectively suppress matching diagnostics, force
them to be at info level, and force them to be at warning level.

## Passing configuration as CLI arguments

It is possible to pass configuration flags with scalar values as CLI arguments,
overriding any potential value set in the configuration file, by prefixing the
qualified name of each attribute with `--settings.`.

For example, the configuration file below

```scala
// .scalafix.conf
DisableSyntax {
  noFinalize = true
}
lint.error.includes = ".*"
lint.error.excludes = "UnusedScalafixSuppression"
```

is equivalent to the following CLI arguments

```
scalafix ... \
  --settings.DisableSyntax.noFinalize=true \
  --settings.lint.error.includes=.* \
  --settings.lint.error.excludes=UnusedScalafixSuppression
```
