---
id: MissingFinal
title: MissingFinal
id: MissingFinal
title: MissingFinal
---

A rewrite and linter rule that makes sure `final` is used where appropriate.

```scala mdoc:passthrough
import scalafix.internal.config._
```

```scala mdoc:passthrough
println(
scalafix.website.rule("MissingFinal", MissingFinalConfig.default)
)
```

#### noLeakingSealed

A non-final class that extends a sealed trait can be extended from another file,
causing the sealed trait to "leak". See

```scala
// file1.scala
sealed trait t
trait a extends t // error: leaking sealed
class c extends t // error: leaking sealed
// file2.scala
trait b extends a // error: leaking sealed
class d extends c // error: leaking sealed
```

#### noLeakingCaseClass

A non-final case class can have surprising behavior when extended. It is
considered a best-practice to make all case classes final.

```scala
// before
case class A(a: Int)
// after
final case class A(a: Int)
```
