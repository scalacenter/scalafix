---
id: community-rules
sidebar_label: Community rules
title: Community rules
---

Many rules have been developed and published by the community.
Follow [this documentation](external-rules.md) to use them in your project.

> Help us increase visibility & foster collaboration by
> [submitting your favorite rule(s) via pull requests](https://github.com/scalacenter/scalafix/edit/main/docs/rules/community-rules.md)!
## Hygiene rules

Hygiene rules enforce conventions, failing the build on violation or
rewriting the code when possible.

| Repository | Artifact | Description |
| - | - | - |
[ghostbuster91/scalafix-unified](https://github.com/ghostbuster91/scalafix-unified) | `io.github.ghostbuster91.scalafix-unified:unified` | Set of opinionated rules to unify your codebase
[jatcwang/scalafix-named-params](https://github.com/jatcwang/scalafix-named-params) | `com.github.jatcwang:scalafix-named-params` | Add named parameters for your constructor and method calls
[liancheng/scalafix-organize-imports](https://github.com/liancheng/scalafix-organize-imports) | `com.github.liancheng:organize-imports` | Help you organize Scala import statements
[vovapolu/scaluzzi](https://github.com/vovapolu/scaluzzi) | `com.github.vovapolu:scaluzzi` | Ensure a subset of [scalazzi](http://yowconference.com.au/slides/yowwest2014/Morris-ParametricityTypesDocumentationCodeReadability.pdf)
[xuwei-k/scalafix-rules](https://github.com/xuwei-k/scalafix-rules) | `com.github.xuwei-k:scalafix-rules` | Avoid ambiguous or redundant Scala syntax & features

## Migration rules

Migration rules make it easier for library users to cope with
deprecations and breaking changes.

Official migrations provided by library authors are not
advertized here as they are usually well documented in the
project itself. Note that 
[Scala Steward](https://github.com/scala-steward-org/scala-steward)
[keeps track of many of them](https://github.com/scala-steward-org/scala-steward/blob/main/modules/core/src/main/resources/scalafix-migrations.conf),
to provide a seamless experience to library users opting-in
for the service. 

| Repository | Artifact | Description |
| - | - | - |
[ohze/scala-rewrites](https://github.com/ohze/scala-rewrites) | `com.sandinh:scala-rewrites` | Rewrites for Scala
[OlegYch/enumeratum-scalafix](https://github.com/OlegYch/enumeratum-scalafix) | `io.github.olegych:enumeratum-scalafix` | Replace `scala.Enumeration` with enumeratum
[scala/scala-collection-compat](https://github.com/scala/scala-collection-compat) | `org.scala-lang.modules:scala-collection-migrations` | Rewrite upgrades to the 2.13 collections
[scala/scala-rewrites](https://github.com/scala/scala-rewrites) | `org.scala-lang:scala-rewrites` | Rewrites for Scala
[xuwei-k/play-ws-scalafix](https://github.com/xuwei-k/play-ws-scalafix) | `com.github.xuwei-k:play-ws-scalafix` | Migrate to play-ws-standalone
[xuwei-k/replace-symbol-literals](https://github.com/xuwei-k/replace-symbol-literals) | `com.github.xuwei-k:replace-symbol-literals` | Replace deprecated scala.Symbol literals `s/'foo/Symbol("foo")/`

## Code generation rules

Code generation rules extend the possibilities of the Scala language
by taking a route similar, yet parallel to macros.

| Repository | Artifact | Description |
| - | - | - |
[earldouglas/linear-scala](https://github.com/earldouglas/linear-scala) | `com.earldouglas:linear-scala-scalafix` | Add support for linear types in Scala
[rtimush/zio-magic-comments](https://github.com/rtimush/zio-magic-comments) | `com.timushev:zio-magic-comments` | Add explanatory graphs as comments to zio-magic methods
[sake92/kalem](https://github.com/sake92/kalem) | `ba.sake:kalem-rules` | Generate `with*` methods for classes
[typelevel/simulacrum-scalafix](https://github.com/typelevel/simulacrum-scalafix) | `org.typelevel:simulacrum-scalafix-annotations` | Simulacrum as Scalafix rules
