---
id: overview
sidebar_label: All rules
title: Built-in Rules
---

Scalafix comes with a small set of built-in rules. Rules are either
**syntactic** or **semantic**.

**Syntactic**: the rule can run directly on source code without compilation.
Syntactic rules are simple to run but they can only do limited code analysis
since they do not have access to information such as symbols and types.

**Semantic**: the rule requires input sources to be compiled beforehand with the
Scala compiler and the
[SemanticDB](https://scalameta.org/docs/semanticdb/guide.html) compiler plugin
enabled. Semantic rules are more complicated to run but they can do more
advanced code analysis based on symbols and types.

```scala mdoc:scalafix-rules

```
