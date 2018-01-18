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
  Jumpstart with our [scalacenter/scalafix.g8][scalafix.g8] template,
  which sets up a minimal sbt project with testing scaffolding.

- __Accessible__: Scalafix enables novices to implement advanced rules
  without learning compiler internals.

- __Portable__: Scalafix works with a wide range of Scala dialects (sbt,
  Scala 2.x, Dotty) and runs on JVM, Node.js and in the browser.

Scalafix is developed at the [Scala Center](https://scala.epfl.ch) with the goal to help automate
migration between different Scala compiler and library versions.
The project follows the Advisory Board proposal: [Clarification of Scala to Dotty migration path](http://scala-lang.org/blog/2016/05/30/scala-center-advisory-board.html#the-first-meeting)

## Links

- September 19th 2017: [Scalafix workshop](https://www.youtube.com/watch?v=uaMWvkCJM_E) at Scala World ([slides](https://speakerdeck.com/gabro/move-fast-and-fix-things))
- September 11th 2017: [Catch bugs with Scalafix](http://www.scala-lang.org/blog/2017/09/11/scalafix-v0.5.html) at scala-lang.org
- May 3rd 2017: [Move fast and refactor with scalafix](https://vimeo.com/channels/flatmap2017/216469977) at flatMap(Oslo)
- March 2nd 2017: [Refactoring with scalafix and scalameta](https://www.youtube.com/watch?v=7I18pJ6orrI) at Scalasphere
- February 27 2017: [Refactor with scalafix v0.3](http://www.scala-lang.org/blog/2017/02/27/scalafix-v0.3.html) at scala-lang.org
- December 12 2016: See the [talk about scalafix at Scala eXchange](https://skillsmatter.com/skillscasts/9117-smooth-migrations-to-dotty-with-scalafix#video) at Scala eXchange (requires login)
- October 24 2016: Read the [v0.1 release announcement](http://www.scala-lang.org/blog/2016/10/24/scalafix.html) at scala-lang.org

[scalafix.g8]: https://github.com/scalacenter/scalafix.g8
