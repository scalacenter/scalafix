---
layout: docs
title: FixSyntax
---

# FixSyntax

_Since 0.5.8_

This rule contains several sub-rules to rewrite "unsafe" syntax. 

## Configuration

By default, this rule does not rewrite syntax.

It contains the following sub-rules:

* `removeFinalVal` transforms `final val` to `val`
* `addFinalCaseClass` transforms `case class` to `final case class`

To rewrite a syntax:

```
FixSyntax.removeFinalVal = true
FixSyntax.addFinalCaseClass = true
```