---
id: related-projects
title: Related projects
---

There are several alternative tools to Scalafix that each make different
tradeoffs with regards to Scala 2.x fidelity, ease of writing custom analyses,
interactivity, performance, integrations and feature support. The table below
provides a rough comparison, below are more detailed explanations.

|                   | Syntax model   | Typechecker    | Linting | Refactoring | Run on compile |
| ----------------- | -------------- | -------------- | ------- | ----------- | -------------- |
| Scalafix          | Scalameta      | Scala compiler | Yes     | Yes         | No             |
| WartRemover       | Scala compiler | Scala compiler | Yes     | No          | Yes            |
| IntelliJ Scala    | IntelliJ Scala | IntelliJ Scala | Yes     | Yes         | No             |
| ScalaStyle        | Scalariform    | n/a            | Yes     | No          | Yes            |
| Scala Refactoring | Scala compiler | Scala compiler | Yes     | Yes         | No             |

## WartRemover

[WartRemover](http://www.wartremover.org/) is a flexible scala linter. Scalafix
and WartRemover can be used together as linters. The primary difference between
Scalafix and WartRemover is that Scalafix runs _after_ compilation as a separate
tool and WartRemover runs during compilation as a compiler plugin. There are no
plans to support lint-on-compile in Scalafix since this use-case is already
served well by WartRemover. See
[Lessons from Building Static Analysis Tools at Google](https://cacm.acm.org/magazines/2018/4/226371-lessons-from-building-static-analysis-tools-at-google/fulltext)
for an in-depth write-up about lint-on-compile vs. lint-after-compile.

- WartRemover has more linter rules out of the box than Scalafix.
- It is easier to write/test/share/run new/custom rules with Scalafix. The
  Scalafix API does not require familiarity with scalac compiler internals.
- Scalafix is a linter and refactoring tool, which means Scalafix rules can
  automatically fix some discovered problems. WartRemover is only a linter, it
  does not support automatic fixing.
- Scalafix supports syntactic rules without going through the compiler, this
  means that WartRemover rules like `Null`, `Throw`, `FinalVal`, `asInstanceOf`,
  `ExplicitImplicitTypes`, `Var`, `While` can run faster outside of compilation
  if implemented with Scalafix.

## IntelliJ Scala

The IntelliJ Scala Plugin is probably the most used IDE for Scala development,
supporting a wide range of features to browse/edit/refactor Scala code.

- Scalafix is primarily aimed to be used in batch-mode through a console
  interface while IntelliJ is primarily aimed for interactive use in the
  IntelliJ IDE.
- IntelliJ Scala contains a lot more rules ("inspections" in IntelliJ terms),
  including sophisticated refactorings such as "Organize imports" and "Move
  class" that Scalafix does not currently support.
- Scalafix uses the Scala 2.x compiler to resolve symbols/types while the
  IntelliJ Scala plugin uses its own typechecker.

## Scalastyle

[Scalastyle](http://www.scalastyle.org/) is a Scala style checker.

- Scalastyle has a lot more rules than Scalafix out of the box.
- Scalafix supports semantic and syntactic rules while Scalastyle supports only
  syntactic rules. Semantic Scalafix rules know more about the types/symbols in
  your source code. For example, a semantic rules can tell if `get` in `x.get`
  comes from `Option[T].get` or from another class that happens to also have a
  `.get` method. Scalastyle is purely syntactic, which means it does not support
  rules that need information about types/symbols.
- Scalastyle runs as a separate tool outside of compilation, just like Scalafix.

## Scala Refactoring

[Scala Refactoring](https://github.com/scala-ide/scala-refactoring) is a library
providing automated refactoring support for Scala.

- Scalafix rules uses the [Scalameta](http://scalameta.org/) AST while Scala
  Refactoring uses the Scala 2.x compiler AST. The Scalameta AST encodes more
  syntactic details from the original source file, including for-comprehensions,
  infix operators and primary constructors. Example:

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

- Scala Refactoring requires a live instance of scala-compiler while
  scalafix-core does not depend on scala-compiler. Scalafix only depends on
  [SemanticDB](http://scalameta.org/tutorial/#SemanticDB). This has positive and
  negative implications, for example
  - Scala Refactoring has always access to the full compiler APIs, while changes
    to the Scalafix semantic API may require changing the Scalameta SemanticDB
    schema.
  - scalafix-core cross-builds to Scala.js, you can interactively explore the
    Scalameta ASTs in the browser on
    [astexplorer.net](https://astexplorer.net/#/gist/f0816de84a02654b8242de5822e672a2/8a7007dd2b292b955e005704f5823c24fab9bfeb).
  - The edit/run/debug cycle when developing Scalafix rules can be very fast if
    you have a pre-built SemanticDB. For example, in
    [olafur/scala-experiments](https://github.com/olafurpg/scala-experiments)
    you can run semantic analysis on a corpus of 2.5M LOC in under 5 seconds
    (more complicated analyses can take 1-2 minutes). You can use that corpus to
    fuzz your Scalafix rules. In Scala Refactoring, you must always re-compile
    sources to test new changes in the rule you are developing.
  - scala-compiler APIs expose internals of the Scala compiler, which requires a
    certain level of expertise to accomplish even fairly simple tasks. It is
    easy to get cryptic compiler errors in scala-compiler if you accidentally
    break some assumed invariants. SemanticDB on the other hand is a plain data
    schema, essentially a small hierarchy of case classes. The entire schema is
    defined in
    [60 lines of protobuf](https://github.com/scalameta/scalameta/blob/master/langmeta/shared/src/main/protobuf/semanticdb.proto).
