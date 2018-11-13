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

1. The first way has a simple object providing an `id`, `pattern`, and `message`.
2. The second way is just the pattern.  When this is used, the `id` is set equal 
   to the pattern, and a generic message is provided for you.
3. The third way allows you to specify what capture-group the problematic thing 
   is in, in case your regex is complicated.

### Error Messages  

Error messages have access to the capture groups of the regex. Simply use `{$n}` 
where `n` is the index of the capture group you wish to appear in that part of 
the message.

You can see this used in the 3rd example.
