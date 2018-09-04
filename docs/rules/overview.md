---
id: overview
sidebar_label: All rules
title: Built-in Rules
---

Scalafix comes with a small set of built-in rules. Some rules are **syntactic**
while other are **semantic**.

**Syntactic**: the rule can run directly on source code without compilation.
Syntactic rules are simple to run and they are typically fast but they do not
have access to important information such as symbols and types.

**Semantic**: the rule can requires the source code to be compiled with the
Scala compiler beforehand with the
[SemanticDB](https://github.com/scalameta/scalameta/blob/master/semanticdb/semanticdb3/semanticdb3.md)
compiler plugin. Semantic rulesa are more complicated to run and they are
typically slower but they can do more advanced analysis based on symbols and
types.

```scala mdoc:scalafix-rules

```
