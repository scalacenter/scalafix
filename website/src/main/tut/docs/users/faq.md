---
layout: docs
title: FAQ
---

# FAQ
{:.no_toc}
If you have any questions, don't hesitate to ask on {% gitter %}.

* TOC
{:toc}

## How does Scalafix compare with alternatives?

There are many alternative tools to Scalafix, that each make different
tradeoffs with regards to Scala 2.x fidelity, ease of writing custom
analyses, interactivity, performance, integrations and feature support.
The table below provides a rough comparison, below are more detailed
explanations.

|              | Scalafix       | IntelliJ Scala | Scala Refactoring | WartRemover    | ScalaStyle  |
| -------      | --------       | -------------- | ----------------- | -----------    | ----------  |
| Syntax model | Scalameta      | IntelliJ Scala | Scala Compiler    | Scala Compiler | Scalariform |
| Typechecker  | Scala Compiler | IntelliJ Scala | Scala Compiler    | Scala Compiler | n/a         |
| Linting      | Yes            | Yes            | Yes               | Yes            | Yes         |
| Refactoring  | Yes            | Yes            | Yes               | No             | No          |

### IntelliJ Scala

The IntelliJ Scala Plugin is probably the most used IDE for Scala development,
supporting a wide range of features to browse/edit/refactor Scala code.

* Scalafix uses the Scala 2.x compiler to resolve symbols/types while
  the IntelliJ Scala plugin uses it's own own typechecker.
  This means that if a project compiles with the Scala compiler,
  then scalafix can analyze the source code (assuming no conflicting compiler plugins).
  The IntelliJ Scala typechecker is known to produce false red squigglies.
  It depends on what Scala features you use whether the IntelliJ Scala can analyze
  your source code.
* Scalafix is primarily aimed to be used in batch-mode through a console interface
  while IntelliJ is primarily aimed for interactive use in the IntelliJ IDE.
* IntelliJ Scala contains a lot more rules ("inspections" in IntelliJ terms),
  including sophisticated refactorings such as "Organize imports" and
  "Move class" that Scalafix does not currently support.


The IntelliJ Scala Plugin will reportedly soon release a "Migrators API",
according to this release: https://blog.jetbrains.com/scala/2016/11/11/intellij-idea-2016-3-rc-scala-js-scala-meta-and-more/
We look forward to see more of it!

### WartRemover
[WartRemover](http://www.wartremover.org/) is a flexible scala linter.

- Scalafix runs *after* compilation as a separate tool,
  WartRemover runs during compilation as a compiler plugin.
- Compilation overhead of the semanticdb-scalac compiler plugin (that Scalafix requires)
  is ~20-30%, which is likely higher than the WartRemover compiler plugin (I haven't measured).
  Performance in semanticdb-scalac will be addressed in future releases.
- WartRemover has more linter rules out of the box than Scalafix.
- It is easier to write/test/share/run new/custom rules with Scalafix.
  The Scalafix API does not require familiarity with scalac compiler internals.
- Scalafix is a linter and refactoring tool, which means Scalafix rules
  can automatically fix some discovered problems. WartRemover is only a linter, it
  does not support automatic fixing.
- Scalafix supports syntactic rules without going through the compiler,
  this means that WartRemover rules like Null, Throw, FinalVal, asInstanceOf,
  ExplicitImplicitTypes, Var, While can run faster outside of compilation
  if implemented with Scalafix.

### Scalastyle

[Scalastyle](http://www.scalastyle.org/) is a Scala style checker.

* Scalastyle has a lot more rules than Scalafix out of the box.
* Scalafix supports semantic and syntactic rules while Scalastyle supports only syntactic rules.
  Semantic Scalafix rules know more about the types/symbols in your source code.
  For example, a semantic rules can tell if `get` in `x.get` comes from `Option[T].get` or from another class that happens to also have a `.get` method.
  Scalastyle is purely syntactic, which means it does not support rules that need information about types/symbols.
* Scalastyle runs as a separate tool outside of compilation, just like Scalafix.

### Scala Refactoring?

[Scala Refactoring](https://github.com/scala-ide/scala-refactoring) is a library providing automated refactoring support for Scala.

* Scalafix rules uses the [Scalameta](http://scalameta.org/) AST while Scala
  Refactoring uses the Scala 2.x compiler AST.
  The Scalameta AST encodes more syntactic details from the original
  source file, including for-comprehensions, infix operators and
  primary constructors. Example:
```scala
// scalameta AST
case class A(b: Int, c: String) {
  for {
    char <- c
    if char == 'a'
    i <- (1 to char.toInt)
  } yield i
  val d, List(e: Int) = List(1)
}
// Scala Compiler AST
package <empty> {
  case class A extends scala.Product with scala.Serializable {
    <caseaccessor> <paramaccessor> val b: Int = _;
    <caseaccessor> <paramaccessor> val c: String = _;
    def <init>(b: Int, c: String) = {
      super.<init>();
      ()
    };
    c.withFilter(((char) => char.$eq$eq('a'))).flatMap(((char) => 1.to(char.toInt).map(((i) => i))));
    val d = List(1);
    val e: Int = List(1): @scala.unchecked match {
      case List((e @ (_: Int))) => e
    }
  }
}
```
* Scala Refactoring requires a live instance of scala-compiler while
  scalafix-core does not depend on scala-compiler. Scalafix only depends
  on [SemanticDB](http://scalameta.org/tutorial/#SemanticDB). This has
  positive and negative implications, for example
  - Scala Refactoring has always access to the full compiler APIs,
    while changes to the Scalafix semantic API may require changing the
    Scalameta SemanticDB schema.
  - scalafix-core cross-builds to Scala.js, you can interactively explore
    the Scalameta ASTs in the browser on [astexplorer.net](https://astexplorer.net/#/gist/f0816de84a02654b8242de5822e672a2/8a7007dd2b292b955e005704f5823c24fab9bfeb).
  - The edit/run/debug cycle when developing Scalafix rules can be very fast
    if you have a pre-built SemanticDB.
    For example, in
    [olafur/scala-experiments](https://github.com/olafurpg/scala-experiments)
    you can run semantic analysis on a corpus of 2.5M LOC in under 5 seconds
    (more complicated analyses can take 1-2 minutes).
    You can use that corpus to fuzz your Scalafix rules.
    In Scala Refactoring, you must always re-compile sources to test
    new changes in the rule you are developing.
  - scala-compiler APIs expose internals of the Scala compiler, which
    requires a certain level of expertise to accomplish even fairly
    simple tasks. It is easy to get cryptic compiler errors in scala-compiler
    if you accidentally break some assumed invariants.
    SemanticDB on the other hand is a plain data schema, essentially
    a small hierarchy of case classes. The entire schema is defined in
    [60 lines of protobuf](https://github.com/scalameta/scalameta/blob/master/langmeta/shared/src/main/protobuf/semanticdb.proto).

## I get resolution errors for org.scalameta:semanticdb-scalac
Make sure you are using a supported Scala version: {{ site.supportedScalaVersions | join: ", " }}.
Note, the version must match exactly, including the last number.

## Enclosing tree [2873] does not include tree [2872]

Scalafix requires code to compile with the scalac option `-Yrangepos`.
A macro that emits invalid tree positions is usually the cause of compiler errors
triggered by `-Yrangepos`. Other tools like the presentation compiler (ENSIME/Scala IDE) require
`-Yrangepos` to work properly.

## I get exceptions about coursier
If you use sbt-coursier, make sure you are on version {{ site.coursierVersion }}.

## Scalafix doesn't do anything
- If you use sbt-scalafix, try {% doc_ref Installation, Verify sbt installation %}
- Make sure that you are running at least one rule.
- Make sure that you are using a supported Scala version: {{ site.supportedScalaVersions | join: ", " }}.

## RemoveUnusedImports does not remove unused imports
Make sure that you followed the instructions in {% rule_ref RemoveUnusedImports %} regarding scalac options.

## IDE support
Scalafix has no IDE support at the moment.
If you are interested in contributing IDE support, please reach out on {% gitter %}!

## sbt runs out of memory

You can control the level of concurrency of the sbt `Task`s with the `Scalafix` [`Tag`](https://www.scala-sbt.org/1.x/docs/Parallel-Execution.html) and the level of concurrency inside scalafix with the `scalafixParallel` setting.

For example, to restrict scalafix to one task at a time but to use all available cores, use 

```scala
concurrentRestrictions in Global += Tags.limit(Scalafix, 1)
scalafixParallel in Global := true
```
