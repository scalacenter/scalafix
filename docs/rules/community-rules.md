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
[xuwei-k/scalafix-rules](https://github.com/xuwei-k/scalafix-rules) | `com.github.xuwei-k::scalafix-rules` | Avoid ambiguous or redundant Scala syntax & features
[vovapolu/scaluzzi](https://github.com/vovapolu/scaluzzi) | `com.github.vovapolu::scaluzzi` | Ensure a subset of [scalazzi](http://yowconference.com.au/slides/yowwest2014/Morris-ParametricityTypesDocumentationCodeReadability.pdf)
[typelevel/typelevel-scalafix](https://github.com/typelevel/typelevel-scalafix) | `org.typelevel::typelevel-scalafix` | Set of rules to provide automated rewrites and linting for code which makes use of Typelevel libraries
[jatcwang/scalafix-named-params](https://github.com/jatcwang/scalafix-named-params) | `com.github.jatcwang::scalafix-named-params` | Add named parameters for your constructor and method calls
[pixiv/scalafix-pixiv-rule](https://github.com/pixiv/scalafix-pixiv-rule) | `net.pixiv::scalafix-pixiv-rule` | Redundant Scala code rewriting and anti-pattern warnings
[dedis/scapegoat-scalafix](https://github.com/dedis/scapegoat-scalafix) | `io.github.dedis::scapegoat-scalafix` | Scalafix implementation of [Scapegoat](https://github.com/scapegoat-scala/scapegoat) linter for Scala 3
[ghostbuster91/scalafix-unified](https://github.com/ghostbuster91/scalafix-unified) | `io.github.ghostbuster91.scalafix-unified::unified` | Set of opinionated rules to unify your codebase
[tanin47/scalafix-forbidden-symbol](https://github.com/tanin47/scalafix-forbidden-symbol) | `io.github.tanin47::scalafix-forbidden-symbol` | Forbid certain packages, classes, methods, and enums in your codebase


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
[scala/scala-collection-compat](https://github.com/scala/scala-collection-compat) | `org.scala-lang.modules::scala-collection-migrations` | Rewrite upgrades to the 2.13 collections
[lightbend-labs/scala-rewrites](https://github.com/lightbend-labs/scala-rewrites) | `org.scala-lang::scala-rewrites` | Rewrites for Scala
[ohze/scala-rewrites](https://github.com/ohze/scala-rewrites) | `com.sandinh::scala-rewrites` | Rewrites for Scala
[xuwei-k/play-ws-scalafix](https://github.com/xuwei-k/play-ws-scalafix) | `com.github.xuwei-k::play-ws-scalafix` | Migrate to play-ws-standalone
[tersesystems/echopraxia-scalafix](https://github.com/tersesystems/echopraxia-scalafix) | `com.tersesystems.echopraxia::scalafix` | Rewrite [Echopraxia](https://github.com/tersesystems/echopraxia) logging statements
[OlegYch/enumeratum-scalafix](https://github.com/OlegYch/enumeratum-scalafix) | `io.github.olegych::enumeratum-scalafix` | Replace `scala.Enumeration` with enumeratum

## Code generation rules

Code generation rules extend the possibilities of the Scala language
by taking a route similar, yet parallel to macros.

| Repository | Artifact | Description |
| - | - | - |
[earldouglas/linear-scala](https://github.com/earldouglas/linear-scala) | `com.earldouglas::linear-scala-scalafix` | Add support for linear types in Scala
[typelevel/simulacrum-scalafix](https://github.com/typelevel/simulacrum-scalafix) | `org.typelevel::simulacrum-scalafix-annotations` | Simulacrum as Scalafix rules
[rtimush/zio-magic-comments](https://github.com/rtimush/zio-magic-comments) | `com.timushev::zio-magic-comments` | Add explanatory graphs as comments to zio-magic methods
[hamnis/dataclass-scalafix](https://github.com/hamnis/dataclass-scalafix) | `net.hamnaberg::dataclass-scalafix` | [alexarchambault/data-class](https://github.com/alexarchambault/data-class) as a Scalafix rule
[sake92/kalem](https://github.com/sake92/kalem) | `ba.sake::kalem-rules` | Generate `with*` methods for classes
