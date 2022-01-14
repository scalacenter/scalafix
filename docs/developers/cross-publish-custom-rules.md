---
id: cross-publish-custom-rules
title: Cross publish custom rules
---
## Already existing custom rules
If your rule is meant only for one scala version, for example, a rule that
automates a migration from one scala version to another, you don't need
to cross publish your rule. In fact, your rule should only be run for
this specific scala version, and should only be published in this
scala version.

Otherwise, if your rule is not specific to a scala version, and in order
to make the migration to scalafix v1 smooth for users, we need the rules
authors to cross-publish their rules for the scala versions they support.
For most rules, this change requires only modifying the build settings as follows:

```diff
 // build.sbt
 scalaVersion := V.scala212,
+crossScalaVersions := List(V.scala213, V.scala212, V.scala211),
```
The second step is to update your CI to run tests on the different
scala versions your rule is being cross-published to. For that, you only
need prefix the test action with `+`. For example:
```
sbt +test
```

## New rules
The scalafix template has already been updated to automatically
cross publish rules (see [pull request](https://github.com/scalacenter/scalafix/issues/1202)).
Therefore, no change is required if you are writing a new rule.



## Projects that already cross-publish
[Edit](https://github.com/scalacenter/scalafix/edit/main/docs/developers/cross-publish-custom-rules.md) this page to submit a pull request that adds more project that cross-publish their rule.
This is a non-exhaustive list of projects and pull requests that have cross-published their rules:
- [liancheng/**scalafix-organize-imports**](https://github.com/liancheng/scalafix-organize-imports/pull/69)
- [scala/**scala-rewrites**](https://github.com/scala/scala-rewrites/pull/33)
- [NeQuissimus/**sort-imports**](https://github.com/NeQuissimus/sort-imports/pull/66)
- [spaceteams/**scalafix-rules**](https://github.com/spaceteams/scalafix-rules)
