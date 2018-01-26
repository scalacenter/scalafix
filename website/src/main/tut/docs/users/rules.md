---
layout: docs
title: Rules
---

# Rules

A Scalafix rule is a small program that can analyze your source code,
report messages and sometimes automatically fix problems.
The following rules come pre-installed with Scalafix.
The "Semantic" column indicates whether the rule requires compilation before running.
Non-semantic rules can run directly from the command line interface without compilation.

```tut:passthrough
println(scalafix.website.allRulesTable)
```

To learn how to extend Scalafix with custom rules, see {% doc_ref Rule Authors %}.

## Planned rules
See [here](https://github.com/scalacenter/scalafix/labels/rule).
