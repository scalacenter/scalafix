---
id: external-rule
sidebar_label: Using external rules
title: Using external rules
---

There have been growing number of scalafix rules published by Scalafix users.

Users can leverage such published rules by following steps.

One method is to run rule dynamically with the following sbt command.

```sh
// sbt shell
> scalafix dependency:RULE@GROUP:ARTIFACT:VERSION
```

Each parts stands for the following.
* `RULE`: A name of rule to be performed.
* `@GROUP:ARTIFACT:VERSION`: An id to specify a dependency containing rules.

To permanently install the rule for a build, users can add the dependency to
`build.sbt` by updating `scalafixDependencies in ThisBuild`.

```scala
// build.sbt
scalafixDependencies in ThisBuild +=
  "GROUP" %% "ARTIFACT" % "VERSION"
// sbt shell
> scalafix RULE
```

You may find useful [scalafix rules in Scaladex](https://index.scala-lang.org/search?q=scalafix).

If you want to create and publish scalafix rules, refer ["Developer guide" section](../developers/setup.md).








