---
layout: docs
id: DisableSyntax
title: DisableSyntax
---

This rule reports errors when a "disallowed" syntax is used. This is a syntactic
rule, which means it does not require compilation to run unlike the `Disable`
rule.

Example:

```scala
MyCode.scala:10: error: [DisableSyntax.throw] exceptions should be avoided,
                        consider encoding the error in the return type instead
  throw new IllegalArgumentException
  ^^^^^
```

## Diagnostic IDs

Each diagnostic is reported under an ID of the form `DisableSyntax.<id>`, as
shown in the example above. That full ID is what you pass to
[suppression](../users/suppression.md) comments and annotations, and to the
`lint.info` / `lint.warning` / `lint.error`
[severity overrides](../users/configuration.md#overriding-the-linting-behavior) in
`.scalafix.conf`:

```scala
val x: Any = null // scalafix:ok DisableSyntax.null
```

IDs are matched as regular expressions against the full ID, not compared for
equality, so a shorter ID that is a prefix of a longer one also matches it. For
example `DisableSyntax.val` matches `DisableSyntax.valPatterns` and
`DisableSyntax.valInAbstract` as well. Anchor the expression, as in
`DisableSyntax.val$`, when you want to match a single ID.

For most settings the ID is the setting name with the `no` prefix dropped, but
not for all of them, so here is every setting that can produce a diagnostic:

| Setting | Diagnostic ID |
| ------- | ------------- |
| `keywords` | the keyword itself, e.g. `import` |
| `noVars` | `var` |
| `noThrows` | `throw` |
| `noNulls` | `null` |
| `noReturns` | `return` |
| `noWhileLoops` | `while` |
| `noAsInstanceOf` | `asInstanceOf`, or `asInstanceOfMatchable` for `asInstanceOf[Matchable]` |
| `noIsInstanceOf` | `isInstanceOf` |
| `noSemicolons` | `semicolons` |
| `noTabs` | `tabs` |
| `noXml` | `xml` |
| `noCovariantTypes` | `covariant` |
| `noContravariantTypes` | `contravariant` |
| `noDefaultArgs` | `defaultArgs` |
| `noValInAbstract` | `valInAbstract` |
| `noImplicitObject` | `implicitObject` |
| `noImplicitConversion` | `implicitConversion` |
| `noFinalVal` | `finalVal` |
| `noFinalize` | `finalize` |
| `noValPatterns` | `valPatterns` |
| `noUniversalEquality` | `==` or `!=`, whichever was used |
| `regex` | the entry's `id`, or the pattern itself when no `id` is given |

Prior to v0.15.0, six of these IDs kept the `no` prefix of their setting name
instead of dropping it. If you suppress or reconfigure the severity of any of
them, rename as follows:

| Before v0.15.0 | Since v0.15.0 |
| -------------- | ------------- |
| `DisableSyntax.noSemicolons` | `DisableSyntax.semicolons` |
| `DisableSyntax.noTabs` | `DisableSyntax.tabs` |
| `DisableSyntax.noXml` | `DisableSyntax.xml` |
| `DisableSyntax.noFinalVal` | `DisableSyntax.finalVal` |
| `DisableSyntax.noFinalize` | `DisableSyntax.finalize` |
| `DisableSyntax.noValPatterns` | `DisableSyntax.valPatterns` |

Configuration setting names are unchanged; only the reported IDs moved.

A stale reference stops matching, so a diagnostic you had suppressed or demoted
is reported again and a previously green build can fail. How much help you get
in finding one depends on where it is:

- `// scalafix:ok` / `// scalafix:off` comments, and `@SuppressWarnings`
  annotations written with the `scalafix:` prefix, also report an
  `UnusedScalafixSuppression` at the stale reference. Note this is a *warning*,
  so on its own it does not fail `--check`.
- `@SuppressWarnings` annotations written *without* the `scalafix:` prefix, such
  as `@SuppressWarnings(Array("DisableSyntax.noXml"))`, are not reported at all —
  unused-suppression warnings are only emitted for prefixed rules.
- Stale `lint.info` / `lint.warning` / `lint.error` entries are silently ignored.

## Configuration

By default, this rule does not disable any particular syntax, every setting is
opt-in.

```scala mdoc:passthrough
import scalafix.internal.rule._
```

```scala mdoc:passthrough
println(
scalafix.website.rule("DisableSyntax", DisableSyntaxConfig.default)
)
```

## Regex

Regex patterns have 3 available ways to be configured.  The example below shows 1 of each way.

```hocon
DisableSyntax.regex = [
  {
    id = offensive
    pattern = "[Pp]imp"
    message = "Please consider a less offensive word than ${0} such as Extension"
  }
  "Await\\.result"
  {
    id = magicNumbers
    regex = {
      pattern = "(?:(?:[,(]\\s)|(?:^\\s*))+(\\d+(\\.\\d+)?)"
      captureGroup = 1
    }
    message = "Numbers ({$1} in this instance) should always have a named parameter attached, or be assigned to a val."
  }
]
```

1. The first way has an object providing an `id`, `pattern`, and `message`.
2. The second way is just the pattern.  When this is used, the `id` is set equal 
   to the pattern, and a generic message is provided for you.
3. The third way allows you to specify what capture-group the problematic piece 
   of code is in, in case your regex is complicated and also matches characters 
   not useful in an error message.

Patterns are matched against the raw source text, including comments. Text
inside [suppression comments](../users/suppression.md) (`// scalafix:ok ...`,
`// scalafix:off ...`, `// scalafix:on ...`) is exempt, so an anchor such as
`// scalafix:ok println` never reports the very pattern it suppresses. Only
matches starting inside such a comment are exempt.

### Error Messages  

Error messages have access to the capture groups of the regex. To access the 
capture groups of the regex, use `{$n}` where `n` is the index of the capture 
group you wish to appear in that part of the message.

You can see this used in the 3rd example.
