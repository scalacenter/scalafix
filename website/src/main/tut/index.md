---
layout: home
title: Home
---

Scalafix is a rewrite and linting tool for Scala.
Key features of Scalafix include:

- __Fidelity__: Scalafix supports formatting-aware rewriting
  of Scala code. Every detail in the source file is represented
  with rich data structures and full position information, down to
  individual space characters.

- __Extensible__: Implement your own custom Scalafix rules.
  Jumpstart with our [scalacenter/scalafix.g8](#scalafix.g8) template,
  which sets up a minimal sbt project with testing scaffolding.

- __Accessible__: Scalafix enables novices to implement advanced rules
  without learning compiler internals.

- __Portable__: Scalafix works with a wide range of Scala dialects (sbt,
  Scala 2.x, Dotty) and runs on JVM, Node.js and in the browser.

Scalafix is developed at the [Scala Center](https://scala.epfl.ch) with the goal to help automate
migration between different Scala compiler and library versions.
The project follows the Advisory Board proposal: [Clarification of Scala to Dotty migration path](http://scala-lang.org/blog/2016/05/30/scala-center-advisory-board.html#the-first-meeting)
